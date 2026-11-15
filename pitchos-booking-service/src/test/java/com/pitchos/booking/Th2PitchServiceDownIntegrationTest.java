package com.pitchos.booking;

import com.pitchos.booking.repository.BookingRepository;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ServerSocket;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH2 - Pitch Service KHÔNG chạy (không có tiến trình nào lắng nghe cổng): Booking trả 503,
 * không tạo đơn và vẫn phục vụ các API không cần Pitch Service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties",
        properties = "pitch.service.url=http://127.0.0.1:${test.closed-port}")
class Th2PitchServiceDownIntegrationTest {

    static {
        System.setProperty("test.closed-port", String.valueOf(findClosedPort()));
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("Pitch Service tắt → POST đặt sân trả 503 (không phải 500), không tạo đơn")
    void pitchDown_returns503() throws Exception {
        String start = LocalDate.now().plusDays(1).atTime(LocalTime.of(18, 0)).toString();
        String end = LocalDate.now().plusDays(1).atTime(LocalTime.of(19, 0)).toString();

        mvc.perform(post("/api/v1/bookings")
                        .header("Authorization", TestTokens.bearer(TestTokens.user(3)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pitchId\":1,\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Dịch vụ quản lý sân hiện không khả dụng. Vui lòng thử lại sau!"));
        assertThat(bookingRepository.count()).isZero();

        // Booking Service không bị sập theo: API không phụ thuộc Pitch vẫn trả 200
        mvc.perform(get("/api/v1/bookings/my-bookings")
                        .header("Authorization", TestTokens.bearer(TestTokens.user(3))))
                .andExpect(status().isOk());
    }

    /** Mở rồi đóng ngay một cổng để chắc chắn không có tiến trình nào đang lắng nghe. */
    private static int findClosedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
