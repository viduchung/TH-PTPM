package com.pitchos.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Body của POST /api/v1/bookings và PUT /api/v1/bookings/{id}.
 * userId KHÔNG gửi trong body mà luôn lấy từ JWT Token (không tin dữ liệu client).
 * Quy tắc thời gian (giờ bắt đầu ở tương lai, thời lượng 30 phút – 8 giờ) kiểm tra ở BookingTimeRules.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "pitchId không được để trống")
    @Positive(message = "pitchId phải là số dương")
    private Long pitchId;

    @NotNull(message = "startTime không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "endTime không được để trống")
    private LocalDateTime endTime;
}
