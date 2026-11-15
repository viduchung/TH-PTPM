package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hợp đồng DTO: Sự kiện tích hợp liên dịch vụ (Integration Event Message).
 * Sử dụng cho giao tiếp bất đồng bộ qua Message Broker (RabbitMQ / Kafka)
 * hoặc đồng bộ REST Webhook.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingIntegrationEventDTO {

    private String eventId;

    /** Loại sự kiện: BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_CANCELLED, PAYMENT_SUCCESS */
    private String eventType;

    private Long bookingId;

    private Long userId;

    private Long pitchId;

    private BigDecimal amount;

    private String idempotencyKey;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
