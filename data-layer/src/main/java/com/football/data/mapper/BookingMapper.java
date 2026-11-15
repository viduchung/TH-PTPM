package com.football.data.mapper;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Mapper an toàn chuyển đổi giữa Entity Booking và các DTO BookingResponseDTO, BookingRequestDTO
 * 
 * NGUYÊN TẮC:
 * - Bảo toàn tính độc lập của Khóa logic: Map thẳng userId và pitchId dạng Long.
 * - Tự động tính toán trường phái sinh `durationHours` để Client và Payment Service không phải tự parse LocalDateTime.
 */
public final class BookingMapper {

    private BookingMapper() {
        // Utility class
    }

    public static BookingResponseDTO toDTO(Booking booking) {
        return toDTO(booking, null);
    }

    public static BookingResponseDTO toDTO(Booking booking, String pitchName) {
        if (booking == null) {
            return null;
        }

        double durationHours = 0.0;
        if (booking.getStartTime() != null && booking.getEndTime() != null) {
            Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
            durationHours = (double) duration.toMinutes() / 60.0;
        }

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .pitchId(booking.getPitchId())
                .pitchName(pitchName)
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .durationHours(durationHours)
                .pricePerHour(booking.getPricePerHour())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .statusDescription(booking.getStatus() != null ? booking.getStatus().getDescription() : null)
                .note(booking.getNote())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public static Booking toEntity(BookingRequestDTO request, BigDecimal pricePerHour, BigDecimal totalPrice) {
        if (request == null) {
            return null;
        }

        return Booking.builder()
                .userId(request.getUserId())
                .pitchId(request.getPitchId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .pricePerHour(pricePerHour)
                .totalPrice(totalPrice)
                .status(BookingStatus.CHO_DUYET)
                .note(request.getNote())
                .build();
    }
}

