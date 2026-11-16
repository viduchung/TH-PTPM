package com.football.bookingservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Khung giờ đã được pitchs-service xác nhận và báo giá (kết quả lời gọi mạng, có trước khi mở transaction). */
public record PricedSlot(Long pitchId, String pitchName, LocalDateTime startTime, LocalDateTime endTime,
                         long totalMinutes, BigDecimal pricePerHour, BigDecimal totalPrice,
                         boolean goldenHourApplied, String calculationBreakdown) {
}
