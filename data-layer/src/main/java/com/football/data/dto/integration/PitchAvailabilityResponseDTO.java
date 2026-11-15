package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

/**
 * Hợp đồng DTO: Kết quả phản hồi trạng thái khả dụng và báo giá từ pitchs-service.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PitchAvailabilityResponseDTO {

    private Long pitchId;

    private String pitchName;

    private boolean available;

    private BigDecimal pricePerHour;

    private BigDecimal estimatedTotalPrice;

    private String message;
}
