package com.football.data.controller;

import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.dto.common.ApiResponse;
import com.football.data.dto.common.PageResponse;
import com.football.data.dto.pitch.PitchResponseDTO;
import com.football.data.exception.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller mẫu minh họa RESTful API nâng cao (TH3)
 * Minh chứng chuẩn định dạng Response (ApiResponse, PageResponse)
 * và cơ chế bắt lỗi tập trung của @ControllerAdvice.
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 3 (TH3)
 */
@RestController
@RequestMapping("/api/test")
public class SampleRestValidationController {

    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> createBooking(
            @Valid @RequestBody BookingRequestDTO request) {

        // Giả lập kịch bản trùng lịch (Câu 2 Ý 3 & TH3)
        if (request.getPitchId() == 999L) {
            throw new ConflictException("Sân này đã có đơn đặt trùng khung giờ thi đấu. Vui lòng chọn giờ khác!");
        }

        // Giả lập kịch bản sân không tồn tại
        if (request.getPitchId() == 404L) {
            throw new ResourceNotFoundException("Sân bóng", "id", request.getPitchId());
        }

        BookingResponseDTO responseDTO = BookingResponseDTO.builder()
                .id(100L)
                .userId(request.getUserId())
                .pitchId(request.getPitchId())
                .pitchName("Sân 7 A Cầu Giấy")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationHours(1.5)
                .pricePerHour(new BigDecimal("400000.00"))
                .totalPrice(new BigDecimal("600000.00"))
                .status("CHO_DUYET")
                .statusDescription("Chờ duyệt")
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn đặt sân thành công", responseDTO));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> getBookingById(@PathVariable Long id) {
        if (id == 404L) {
            throw new ResourceNotFoundException("Đơn đặt sân", "id", id);
        }
        if (id == 403L) {
            throw new ForbiddenException("Bạn không có quyền truy cập vào đơn đặt sân của khách hàng khác");
        }
        if (id == 401L) {
            throw new UnauthorizedException("Phiên đăng nhập đã hết hạn hoặc token không hợp lệ");
        }

        BookingResponseDTO responseDTO = BookingResponseDTO.builder()
                .id(id)
                .userId(1L)
                .pitchId(2L)
                .pitchName("Sân 5 số 1")
                .startTime(LocalDateTime.of(2026, 11, 20, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 20, 19, 30))
                .totalPrice(new BigDecimal("375000.00"))
                .status("DA_DUYET")
                .statusDescription("Đã duyệt")
                .build();

        return ResponseEntity.ok(ApiResponse.ok(responseDTO));
    }

    @GetMapping("/pitches")
    public ResponseEntity<ApiResponse<PageResponse<PitchResponseDTO>>> getPitchesPaging(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PitchResponseDTO p1 = PitchResponseDTO.builder()
                .id(1L).name("Sân 5 số 1").type("SAN_5").pricePerHour(new BigDecimal("250000.00")).available(true).build();
        PitchResponseDTO p2 = PitchResponseDTO.builder()
                .id(2L).name("Sân 7 A").type("SAN_7").pricePerHour(new BigDecimal("450000.00")).available(true).build();

        Page<PitchResponseDTO> pitchPage = new PageImpl<>(List.of(p1, p2), PageRequest.of(page, size), 2);
        PageResponse<PitchResponseDTO> pageResponse = PageResponse.from(pitchPage);

        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách sân thành công", pageResponse));
    }

    @GetMapping("/service-check")
    public ResponseEntity<ApiResponse<String>> checkService(@RequestParam boolean down) {
        if (down) {
            throw new ServiceUnavailableException("pitchs-service");
        }
        return ResponseEntity.ok(ApiResponse.ok("Dịch vụ hoạt động bình thường"));
    }
}

