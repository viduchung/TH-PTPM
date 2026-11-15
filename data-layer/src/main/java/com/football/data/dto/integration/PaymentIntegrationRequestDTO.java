package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Hợp đồng DTO: Yêu cầu xử lý thanh toán gửi từ booking-service sang payment-service.
 * Chứa khóa Idempotency Key để đảm bảo không bị trừ tiền 2 lần.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentIntegrationRequestDTO {

    @NotNull(message = "bookingId không được để trống")
    private Long bookingId;

    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "10000.0", message = "Số tiền tối thiểu là 10,000 VNĐ")
    private BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // VNPAY, MOMO, BANKING

    private String idempotencyKey;

    private String note;
}

