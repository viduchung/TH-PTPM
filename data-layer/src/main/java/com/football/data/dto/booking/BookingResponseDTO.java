package com.football.data.dto.booking;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO Chi tiết Đơn đặt sân (Booking Details Response DTO)
 * Trả về thông tin hoàn chỉnh của đơn đặt sân cho Client, Payment Service, Notification Service.
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 2 (TH2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;
    private Long userId;
    private Long pitchId;
    private String pitchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double durationHours;
    private BigDecimal pricePerHour;
    private BigDecimal totalPrice;
    private String status;
    private String statusDescription;
    private String note;
    private LocalDateTime createdAt;
}

