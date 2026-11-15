package com.football.data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.data.dto.booking.BookingRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("TH3 Test 1: Tạo đơn thành công trả về 201 Created + ApiResponse envelope")
    void testCreateBooking_Success() throws Exception {
        BookingRequestDTO validRequest = BookingRequestDTO.builder()
                .userId(1L)
                .pitchId(2L)
                .startTime(LocalDateTime.of(2026, 11, 20, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 20, 19, 30))
                .note("Giao lưu bóng đá")
                .build();

        mockMvc.perform(post("/api/test/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo đơn đặt sân thành công"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.durationHours").value(1.5))
                .andExpect(jsonPath("$.data.totalPrice").value(600000.00));
    }

    @Test
    @DisplayName("TH3 Test 2: Validation dữ liệu đầu vào thất bại trả về 400 Bad Request + Chi tiết các trường lỗi")
    void testCreateBooking_ValidationError() throws Exception {
        // Gửi request rỗng vi phạm tất cả validation @NotNull
        BookingRequestDTO invalidRequest = new BookingRequestDTO();

        mockMvc.perform(post("/api/test/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
                .andExpect(jsonPath("$.errors.userId").exists())
                .andExpect(jsonPath("$.errors.pitchId").exists())
                .andExpect(jsonPath("$.errors.startTime").exists())
                .andExpect(jsonPath("$.errors.endTime").exists());
    }

    @Test
    @DisplayName("TH3 Test 3: Xung đột trùng lịch thi đấu trả về 409 Conflict + ErrorResponse")
    void testCreateBooking_ConflictSchedule() throws Exception {
        BookingRequestDTO conflictRequest = BookingRequestDTO.builder()
                .userId(1L)
                .pitchId(999L) // 999 giả lập trùng lịch
                .startTime(LocalDateTime.of(2026, 11, 20, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 20, 19, 30))
                .build();

        mockMvc.perform(post("/api/test/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.errorCode").value("ERR_RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("Sân này đã có đơn đặt trùng")));
    }

    @Test
    @DisplayName("TH3 Test 4: Tài nguyên không tìm thấy trả về 404 Not Found + ErrorResponse")
    void testGetBooking_NotFound() throws Exception {
        mockMvc.perform(get("/api/test/bookings/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.errorCode").value("ERR_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Không tìm thấy Đơn đặt sân")));
    }

    @Test
    @DisplayName("TH3 Test 5: Không đủ quyền truy cập tài nguyên trả về 403 Forbidden")
    void testGetBooking_Forbidden() throws Exception {
        mockMvc.perform(get("/api/test/bookings/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    @DisplayName("TH3 Test 6: Phiên đăng nhập hết hạn trả về 401 Unauthorized")
    void testGetBooking_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/test/bookings/401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("TH3 Test 7: Dịch vụ đối tác mất kết nối trả về 503 Service Unavailable")
    void testServiceUnavailable() throws Exception {
        mockMvc.perform(get("/api/test/service-check?down=true"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.errorCode").value("ERR_SERVICE_DOWN"));
    }

    @Test
    @DisplayName("TH3 Test 8: Gọi sai HTTP Method trả về 405 Method Not Allowed")
    void testMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/test/pitches"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    @DisplayName("TH3 Test 9: Phân trang danh sách sân bóng trả về cấu trúc PageResponse chuẩn")
    void testGetPitches_Paging() throws Exception {
        mockMvc.perform(get("/api/test/pitches?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.empty").value(false));
    }
}
