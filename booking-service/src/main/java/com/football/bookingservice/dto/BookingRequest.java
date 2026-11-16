package com.football.bookingservice.dto;

import com.football.bookingservice.entity.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Yêu cầu đặt / sửa đơn (TH1, TH3). Không có userId: người đặt luôn lấy từ JWT,
 * client không thể đặt hộ người khác bằng cách sửa body.
 */
public record BookingRequest(
        @Schema(example = "1")
        @NotNull(message = "pitchId không được để trống")
        @Positive(message = "pitchId phải là số dương")
        Long pitchId,

        @Schema(example = "2026-11-20T18:00:00")
        @NotNull(message = "startTime không được để trống")
        LocalDateTime startTime,

        @Schema(example = "2026-11-20T19:30:00")
        @NotNull(message = "endTime không được để trống")
        LocalDateTime endTime,

        @Size(max = Booking.NOTE_MAX_LENGTH, message = "note tối đa 255 ký tự")
        String note) {
}
