package com.pitchos.booking;

import com.pitchos.booking.support.FakePitchServer;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.pitchos.booking.support.FakePitchServer.delayed;
import static com.pitchos.booking.support.FakePitchServer.ok;
import static com.pitchos.booking.support.FakePitchServer.pitchJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH2 - Booking gọi Pitch Service bằng WebClient và xử lý lỗi; TH6 - Consumer/Provider, retry có giới hạn, báo giá.
 * Pitch Service là HTTP server thật (giả lập) nên lỗi mạng, mã HTTP và độ trễ đều là thật.
 * Cấu hình test: response-timeout 700 ms, tối đa 2 lần thử lại (tổng 3 lần gọi).
 */
class Th2Th6PitchIntegrationTest extends IntegrationTestBase {

    private static final String UNAVAILABLE = "Dịch vụ quản lý sân hiện không khả dụng. Vui lòng thử lại sau!";

    @Test
    @DisplayName("Pitch hoạt động: Booking lấy giá qua WebClient, gọi Provider đúng 1 lần")
    void pitchUp_bookingCreatedWithProviderPrice() throws Exception {
        postBooking(TestTokens.user(USER_A), bookingJson(2, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(500000.00));
        assertThat(PITCH.hits(2)).isEqualTo(1);
    }

    @Test
    @DisplayName("Sân không tồn tại (Pitch trả 404) → 404, KHÔNG thử lại, KHÔNG tạo đơn")
    void pitchNotFound_returns404WithoutRetry() throws Exception {
        postBooking(TestTokens.user(USER_A), bookingJson(99, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy sân bóng với pitchId đã gửi"));
        assertThat(PITCH.hits(99)).isEqualTo(1);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Pitch lỗi 500 → 503, không thử lại (lỗi không tạm thời), không tạo đơn")
    void pitchInternalError_returns503WithoutRetry() throws Exception {
        PITCH.setDefault(1, FakePitchServer.status(500));

        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(UNAVAILABLE));
        assertThat(PITCH.hits(1)).isEqualTo(1);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Pitch lỗi 503 tạm thời 2 lần rồi phục hồi → retry thành công, 201 (tổng 3 lần gọi)")
    void pitchTransient503_retriedThenSucceeds() throws Exception {
        PITCH.enqueue(1, FakePitchServer.status(503), FakePitchServer.status(503));

        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isCreated());
        assertThat(PITCH.hits(1)).isEqualTo(3);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Pitch 503 liên tục → retry có giới hạn (đúng 3 lần gọi) rồi trả 503, không tạo đơn")
    void pitchAlways503_retryIsBounded() throws Exception {
        PITCH.setDefault(1, FakePitchServer.status(503));

        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable());
        assertThat(PITCH.hits(1)).isEqualTo(3);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Pitch phản hồi chậm hơn timeout → 503 trong thời gian có giới hạn, Booking không bị treo")
    void pitchTimeout_returns503Quickly() throws Exception {
        PITCH.setDefault(1, delayed(pitchJson(1, "300000.00", "hoat_dong", null), 3_000));

        long startedAt = System.nanoTime();
        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable());
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        // 3 lần × 700 ms timeout + backoff ≈ 2,2 s; thấp hơn nhiều so với 3 × 3 s nếu chờ hết
        assertThat(elapsedMs).isLessThan(4_000);
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Dữ liệu sân không hợp lệ (thiếu giá / giá 0 / id lệch / JSON hỏng) → 503, không tạo đơn")
    void invalidPitchData_returns503() throws Exception {
        PITCH.setDefault(1, ok(pitchJson(1, null, "hoat_dong", null)));
        PITCH.setDefault(2, ok(pitchJson(2, "0", "hoat_dong", null)));
        PITCH.setDefault(5, ok(pitchJson(77, "300000.00", "hoat_dong", null)));
        PITCH.setDefault(6, ok("{not-json"));

        for (long pitchId : new long[]{1, 2, 5, 6}) {
            postBooking(TestTokens.user(USER_A), bookingJson(pitchId, at(0, 18, 0), at(0, 19, 0)))
                    .andExpect(status().isServiceUnavailable());
        }
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Sân đang bảo trì (bao_tri) → 400, không tạo đơn")
    void pitchUnderMaintenance_returns400() throws Exception {
        postBooking(TestTokens.user(USER_A), bookingJson(3, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sân đang bảo trì, không thể đặt"));
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /quote: báo giá từ Provider, không tạo đơn; available = false khi khung giờ đã bị giữ")
    void quote_returnsPriceAndAvailability() throws Exception {
        String quoteUrl = "/api/v1/bookings/quote?pitchId=1&startTime=" + at(0, 18, 0) + "&endTime=" + at(0, 20, 0);

        getWithToken(quoteUrl, TestTokens.user(USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricePerHour").value(300000.00))
                .andExpect(jsonPath("$.durationMinutes").value(120))
                .andExpect(jsonPath("$.totalPrice").value(600000.00))
                .andExpect(jsonPath("$.available").value(true));
        assertThat(bookingRepository.count()).isZero();

        createBooking(USER_B, 1, at(0, 19, 0), at(0, 20, 0));
        getWithToken(quoteUrl, TestTokens.user(USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("Sau khi Pitch hết lỗi, Booking hoạt động lại bình thường (không bị sập theo)")
    void bookingRecoversAfterProviderFailure() throws Exception {
        PITCH.setDefault(1, FakePitchServer.status(500));
        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable());

        PITCH.reset();
        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isCreated());
    }
}
