package com.football.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.football.bookingservice.entity.Booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Đơn đặt sân trả cho client - cùng tên trường với BookingResponseDTO của data-layer.
 * pitchName chỉ có khi vừa gọi pitchs-service (tạo / sửa đơn); booking_db không lưu tên sân.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponse(Long id, Long userId, Long pitchId, String pitchName,
                              LocalDateTime startTime, LocalDateTime endTime, Double durationHours,
                              BigDecimal pricePerHour, BigDecimal totalPrice, String status,
                              String statusDescription, String note,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {

    private static final int HOURS_SCALE = 2;
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    public static BookingResponse from(Booking booking) {
        return from(booking, null);
    }

    public static BookingResponse from(Booking booking, String pitchName) {
        return new BookingResponse(booking.getId(), booking.getUserId(), booking.getPitchId(), pitchName,
                booking.getStartTime(), booking.getEndTime(),
                durationHours(booking.getStartTime(), booking.getEndTime()),
                booking.getPricePerHour(), booking.getTotalPrice(),
                booking.getStatus().name(), booking.getStatus().getDescription(), booking.getNote(),
                booking.getCreatedAt(), booking.getUpdatedAt());
    }

    private static Double durationHours(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        return BigDecimal.valueOf(minutes).divide(MINUTES_PER_HOUR, HOURS_SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
