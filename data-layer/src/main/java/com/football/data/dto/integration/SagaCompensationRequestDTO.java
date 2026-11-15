package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Hợp đồng DTO: Yêu cầu thực hiện giao dịch bù trừ (Compensating Transaction / Rollback).
 * Kích hoạt khi quy trình tích hợp phân tán gặp sự cố (ví dụ: trừ tiền thành công nhưng sân bị khóa).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SagaCompensationRequestDTO {

    @NotNull(message = "bookingId không được để trống")
    private Long bookingId;

    @NotNull(message = "Lý do hủy/bù trừ không được để trống")
    private String reason;

    private boolean refundRequired;

    private BigDecimal refundAmount;
}

