package com.football.data.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO Yêu cầu đặt sân mới (Booking Request DTO)
 * Chứa dữ liệu đầu vào từ Client gửi lên hoặc Payment Service gọi sang.
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 2 (TH2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {

    @NotNull(message = "Mã người dùng (userId) không được để trống")
    private Long userId;

    @NotNull(message = "Mã sân bóng (pitchId) không được để trống")
    private Long pitchId;

    @NotNull(message = "Thời gian bắt đầu nhận sân không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc trả sân không được để trống")
    private LocalDateTime endTime;

    private String note;
}

