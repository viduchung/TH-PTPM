package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hợp đồng DTO: Kết quả xử lý thanh toán từ payment-service.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentIntegrationResponseDTO {

    private Long paymentId;

    private Long bookingId;

    private BigDecimal amount;

    private String status; // SUCCESS, FAILED, PENDING

    private String transactionCode;

    private boolean success;

    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
