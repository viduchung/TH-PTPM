package com.football.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Báo giá trước khi đặt (TH6): giá do pitchs-service tính, available do booking-service kiểm tra lịch. */
public record QuoteResponse(Long pitchId, String pitchName, LocalDateTime startTime, LocalDateTime endTime,
                            long totalMinutes, BigDecimal pricePerHour, BigDecimal totalPrice,
                            boolean goldenHourApplied, String calculationBreakdown, boolean available) {
}
