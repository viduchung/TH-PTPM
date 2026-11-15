package com.pitchos.booking.dto;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private Long userId;
    private Long pitchId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private LocalDateTime createdAt;
    // Các trường bổ sung (TH4): không làm thay đổi các trường cũ ở trên
    private BigDecimal pricePerHour;
    private LocalDateTime updatedAt;

    public static BookingResponse from(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .pitchId(booking.getPitchId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .pricePerHour(booking.getPricePerHour())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
