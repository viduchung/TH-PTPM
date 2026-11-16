package com.football.bookingservice;

import com.football.bookingservice.support.FakeHttpService.Reply;
import com.football.bookingservice.support.IntegrationTestBase;
import com.football.bookingservice.support.TestTokens;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.football.bookingservice.support.FakePitchService.pitchKey;
import static com.football.bookingservice.support.FakePitchService.quoteKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH2 - booking gọi pitchs-service (WebClient) kiểm tra sân + lấy giá.
 * TH6 - Consumer của /quote: xử lý lỗi provider, retry lỗi tạm thời, không tạo đơn khi không xác minh được sân.
 */
class Th2Th6PitchIntegrationTest extends IntegrationTestBase {

    @Test
    void createCallsPitchAndQuoteWithInternalServiceKey() throws Exception {
        createBooking(CUSTOMER_A, 2, at(0, 18, 0), at(0, 19, 0));

        assertThat(PITCH.hits(pitchKey(2))).isEqualTo(1);
        assertThat(PITCH.hits(quoteKey(2))).isEqualTo(1);
        assertThat(PITCH.lastHeader("X-Internal-Service-Key")).isEqualTo(TestTokens.INTERNAL_KEY);
        assertThat(bookingRepository.findAll().get(0).getTotalPrice()).isEqualByComparingTo("500000.00");
    }

    @Test
    void unknownPitchReturns404AndCreatesNothing() throws Exception {
        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(99, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy sân bóng id = 99"));

        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void pitchUnderMaintenanceIsRejectedWithoutCallingQuote() throws Exception {
        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(3, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sân 'San 3' đang bảo dưỡng, không thể đặt"));

        assertThat(PITCH.hits(quoteKey(3))).isZero();
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void transient503IsRetriedThenSucceeds() throws Exception {
        PITCH.enqueue(pitchKey(1), Reply.status(503), Reply.status(503));

        createBooking(CUSTOMER_A, 1, at(0, 18, 0), at(0, 19, 0));

        assertThat(PITCH.hits(pitchKey(1))).isEqualTo(3);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void pitchStillDownAfterRetriesReturns503AndCreatesNothing() throws Exception {
        PITCH.enqueue(pitchKey(1), Reply.status(503), Reply.status(503), Reply.status(503));

        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("ERR_SERVICE_DOWN"));

        assertThat(PITCH.hits(pitchKey(1))).isEqualTo(3);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void provider500IsNotRetried() throws Exception {
        PITCH.enqueue(pitchKey(1), Reply.status(500));

        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable());

        assertThat(PITCH.hits(pitchKey(1))).isEqualTo(1);
    }

    @Test
    void slowPitchTimesOutInsteadOfHanging() throws Exception {
        String slowBody = "{\"id\":1,\"name\":\"San 1\",\"pricePerHour\":300000,\"status\":\"AVAILABLE\"}";
        PITCH.enqueue(pitchKey(1), Reply.delayed(slowBody, 1500), Reply.delayed(slowBody, 1500),
                Reply.delayed(slowBody, 1500));
        long startedAt = System.nanoTime();

        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable());

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(elapsedMs).as("3 lần thử x timeout 700ms, không chờ hết 4,5 giây").isLessThan(4000);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void invalidPitchPayloadIsTreatedAsUnavailable() throws Exception {
        PITCH.enqueue(pitchKey(1), Reply.ok("{\"id\":1,\"name\":\"San 1\",\"pricePerHour\":null}"));

        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("pitchs-service trả dữ liệu sân không hợp lệ"));
    }

    @Test
    void quote400MessageFromProviderIsPassedThrough() throws Exception {
        PITCH.enqueue(quoteKey(1), new Reply(400, "{\"status\":400,\"message\":\"Thời lượng thuê sân tối thiểu là 30 phút!\"}", 0));

        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thời lượng thuê sân tối thiểu là 30 phút!"));
    }

    @Test
    void publicQuoteReturnsPriceAndAvailability() throws Exception {
        LocalDateTime start = at(0, 18, 0);
        createBooking(CUSTOMER_A, 1, start, start.plusHours(1));

        mvc.perform(get("/api/v1/bookings/quote").param("pitchId", "1")
                        .param("startTime", start.plusMinutes(30).toString())
                        .param("endTime", start.plusMinutes(120).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPrice").value(450000.00))
                .andExpect(jsonPath("$.data.totalMinutes").value(90))
                .andExpect(jsonPath("$.data.available").value(false));

        mvc.perform(get("/api/v1/bookings/quote").param("pitchId", "1")
                        .param("startTime", start.plusHours(1).toString())
                        .param("endTime", start.plusHours(2).toString()))
                .andExpect(jsonPath("$.data.available").value(true));
        assertThat(bookingRepository.count()).as("báo giá không tạo đơn").isEqualTo(1);
    }
}
