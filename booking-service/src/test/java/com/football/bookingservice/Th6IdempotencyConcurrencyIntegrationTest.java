package com.football.bookingservice;

import com.football.bookingservice.entity.IdempotencyStatus;
import com.football.bookingservice.support.FakeHttpService.Reply;
import com.football.bookingservice.support.IntegrationTestBase;
import com.football.bookingservice.support.TestTokens;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.football.bookingservice.support.FakePitchService.pitchKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH6 - Idempotency-Key chống tạo đơn trùng khi gửi lại; khóa theo sân chống double-booking khi đặt đồng thời. */
class Th6IdempotencyConcurrencyIntegrationTest extends IntegrationTestBase {

    private static final int THREADS = 8;

    @Test
    void retryWithSameKeyReturnsOriginalBookingWithoutCreatingAnother() throws Exception {
        String token = TestTokens.customer(CUSTOMER_A);
        String json = bookingJson(1, at(0, 18, 0), at(0, 19, 0));

        long firstId = json(postBooking(token, json, "order-123").andExpect(status().isCreated()).andReturn())
                .at("/data/id").asLong();

        postBooking(token, json, "order-123")
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.data.id").value(firstId));

        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(PITCH.hits(pitchKey(1))).as("lần gửi lại không gọi pitchs-service").isEqualTo(1);
        assertThat(idempotencyRepository.findAll()).singleElement()
                .satisfies(record -> {
                    assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
                    assertThat(record.getResourceId()).isEqualTo(firstId);
                    assertThat(record.getResourceType()).isEqualTo("BOOKING_CREATION");
                });
    }

    @Test
    void sameKeyForDifferentSlotIsRejectedWith409() throws Exception {
        String token = TestTokens.customer(CUSTOMER_A);
        postBooking(token, bookingJson(1, at(0, 18, 0), at(0, 19, 0)), "order-456").andExpect(status().isCreated());

        postBooking(token, bookingJson(1, at(0, 20, 0), at(0, 21, 0)), "order-456")
                .andExpect(status().isConflict());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void keysAreScopedPerUser() throws Exception {
        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)), "shared-key")
                .andExpect(status().isCreated());

        postBooking(TestTokens.customer(CUSTOMER_B), bookingJson(2, at(0, 18, 0), at(0, 19, 0)), "shared-key")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(CUSTOMER_B));
        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    void failedAttemptCanBeRetriedWithSameKey() throws Exception {
        String token = TestTokens.customer(CUSTOMER_A);
        String json = bookingJson(1, at(0, 18, 0), at(0, 19, 0));
        PITCH.enqueue(pitchKey(1), Reply.status(503), Reply.status(503), Reply.status(503));

        postBooking(token, json, "order-789").andExpect(status().isServiceUnavailable());
        assertThat(idempotencyRepository.findAll()).singleElement()
                .extracting(record -> record.getStatus()).isEqualTo(IdempotencyStatus.FAILED);

        postBooking(token, json, "order-789").andExpect(status().isCreated());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidKeyFormatIsRejected() throws Exception {
        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)), "key with spaces!")
                .andExpect(status().isBadRequest());
        postBooking(TestTokens.customer(CUSTOMER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)), "k".repeat(65))
                .andExpect(status().isBadRequest());
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void concurrentBookingsOfSameSlotCreateExactlyOneBooking() throws Exception {
        LocalDateTime start = at(0, 18, 0);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            long customerId = 100 + i;
            attempts.add(() -> postBooking(TestTokens.customer(customerId), bookingJson(1, start, start.plusHours(1)))
                    .andReturn().getResponse().getStatus());
        }

        List<Integer> statuses = runTogether(attempts);

        assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
        assertThat(statuses).filteredOn(code -> code == 409).hasSize(THREADS - 1);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrentRetriesWithSameKeyCreateExactlyOneBooking() throws Exception {
        String token = TestTokens.customer(CUSTOMER_A);
        String json = bookingJson(2, at(0, 18, 0), at(0, 19, 0));
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            attempts.add(() -> postBooking(token, json, "double-click").andReturn().getResponse().getStatus());
        }

        List<Integer> statuses = runTogether(attempts);

        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
        assertThat(statuses).allMatch(code -> code == 201 || code == 200 || code == 409);
    }

    private static List<Integer> runTogether(List<Callable<Integer>> attempts) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> attempt : attempts) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return attempt.call();
                }));
            }
            startGate.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }
}
