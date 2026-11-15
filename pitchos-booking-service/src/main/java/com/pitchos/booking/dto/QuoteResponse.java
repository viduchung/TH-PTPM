package com.pitchos.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Báo giá đặt sân (TH6 - phía Consumer): giá lấy từ Pitch Service (Provider), chưa tạo đơn.
 * available = false nghĩa là khung giờ đã có đơn khác giữ tại thời điểm báo giá.
 */
public record QuoteResponse(Long pitchId,
                            String pitchName,
                            BigDecimal pricePerHour,
                            LocalDateTime startTime,
                            LocalDateTime endTime,
                            long durationMinutes,
                            BigDecimal totalPrice,
                            boolean available) {
}
