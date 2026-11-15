package com.pitchos.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.support.FakePitchServer;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH6 - Idempotency và request đồng thời; TH4 - transaction + khóa theo sân trong CSDL.
 * Các test đồng thời gửi request HTTP THẬT tới Booking Service đang chạy trên cổng ngẫu nhiên.
 */
class Th6IdempotencyConcurrencyIntegrationTest extends IntegrationTestBase {

    private static final int PARALLEL_REQUESTS = 10;

    @Test
    @DisplayName("Gửi lại cùng Idempotency-Key → 200, cùng đơn cũ, header Idempotent-Replayed, chỉ 1 đơn trong CSDL")
    void sameKeyTwice_returnsSameBooking() throws Exception {
        String json = bookingJson(1, at(0, 18, 0), at(0, 19, 0));
        String token = TestTokens.user(USER_A);

        String first = postBooking(token, json, "dat-san-001")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(first).get("id").asLong();

        postBooking(token, json, "dat-san-001")
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(firstId));

        assertThat(bookingRepository.count()).isEqualTo(1);
        // Lần gửi lại trả kết quả đã lưu, không gọi lại Pitch Service
        assertThat(PITCH.hits(1)).isEqualTo(1);
    }

    @Test
    @DisplayName("Gửi lại khi Pitch Service đang lỗi vẫn trả đơn đã tạo (không phụ thuộc Provider)")
    void replayWorksWhileProviderIsDown() throws Exception {
        String json = bookingJson(1, at(0, 18, 0), at(0, 19, 0));
        postBooking(TestTokens.user(USER_A), json, "dat-san-002").andExpect(status().isCreated());

        PITCH.setDefault(1, FakePitchServer.status(503));
        postBooking(TestTokens.user(USER_A), json, "dat-san-002").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cùng key nhưng khác nội dung → 409; key sai định dạng → 400; cùng key của khách khác → đơn mới")
    void keyMisuseIsRejected() throws Exception {
        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)), "dat-san-003")
                .andExpect(status().isCreated());

        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 20, 0), at(0, 21, 0)), "dat-san-003")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 20, 0), at(0, 21, 0)), "khoa co dau cach")
                .andExpect(status().isBadRequest());
        postBooking(TestTokens.user(USER_B), bookingJson(2, at(0, 18, 0), at(0, 19, 0)), "dat-san-003")
                .andExpect(status().isCreated());
        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("10 request ĐỒNG THỜI cùng Idempotency-Key → đúng 1 đơn, mọi request nhận cùng id")
    void concurrentSameKey_createsExactlyOneBooking() throws Exception {
        String json = bookingJson(1, at(0, 18, 0), at(0, 19, 0));
        String token = TestTokens.user(USER_A);

        List<HttpResponse<String>> responses = fireConcurrently(i -> post(token, json, "dong-thoi-001"));

        assertThat(responses).allSatisfy(r -> assertThat(r.statusCode()).isIn(200, 201));
        assertThat(responses.stream().filter(r -> r.statusCode() == 201)).hasSize(1);
        assertThat(responses.stream().map(this::bookingId).distinct()).hasSize(1);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("10 khách đặt ĐỒNG THỜI cùng sân, cùng giờ (không key) → đúng 1 đơn 201, 9 request bị từ chối 400")
    void concurrentSameSlot_onlyOneWins() throws Exception {
        String json = bookingJson(1, at(0, 18, 0), at(0, 19, 0));

        List<HttpResponse<String>> responses = fireConcurrently(i -> post(TestTokens.user(100L + i), json, null));

        Map<Integer, Long> byStatus = responses.stream()
                .collect(Collectors.groupingBy(HttpResponse::statusCode, Collectors.counting()));
        assertThat(byStatus).containsEntry(201, 1L).containsEntry(400, (long) PARALLEL_REQUESTS - 1);
        List<Booking> active = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.cho_duyet).toList();
        assertThat(active).hasSize(1);
    }

    @Test
    @DisplayName("10 request đồng thời vào các sân / khung giờ khác nhau: tất cả thành công (khóa chỉ theo từng sân)")
    void concurrentDifferentPitches_allSucceed() throws Exception {
        List<HttpResponse<String>> responses = fireConcurrently(i ->
                post(TestTokens.user(200L + i), bookingJson(i % 2 == 0 ? 1 : 2, at(i, 18, 0), at(i, 19, 0)), null));

        assertThat(responses).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(201));
        assertThat(bookingRepository.count()).isEqualTo(PARALLEL_REQUESTS);
    }

    private List<HttpResponse<String>> fireConcurrently(Function<Integer, HttpRequest> requestFactory)
            throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < PARALLEL_REQUESTS; i++) {
                HttpRequest request = requestFactory.apply(i);
                Callable<HttpResponse<String>> call = () -> {
                    startSignal.await();
                    return client.send(request, HttpResponse.BodyHandlers.ofString());
                };
                futures.add(pool.submit(call));
            }
            startSignal.countDown();
            List<HttpResponse<String>> responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                responses.add(future.get());
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    private HttpRequest post(String token, String json, String idempotencyKey) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/bookings"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", TestTokens.bearer(token))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return builder.build();
    }

    private long bookingId(HttpResponse<String> response) {
        try {
            JsonNode node = objectMapper.readTree(response.body());
            return node.get("id").asLong();
        } catch (Exception ex) {
            throw new IllegalStateException(response.body(), ex);
        }
    }
}
