package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Hợp đồng DTO: Kết quả thực hiện giao dịch bù trừ (Saga Compensation Result).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SagaCompensationResponseDTO {

    private Long bookingId;

    private boolean compensated;

    private String status; // COMPENSATED, REFUNDED, FAILED

    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
