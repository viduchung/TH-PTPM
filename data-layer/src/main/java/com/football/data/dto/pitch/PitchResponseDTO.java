package com.football.data.dto.pitch;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO Chi tiết Sân bóng (Pitch Details DTO)
 * Đại diện thông tin đầy đủ về sân bóng gửi ra ngoài biên giới dịch vụ (Service Boundary).
 * Tránh hoàn toàn việc lộ Entity JPA và tránh hiện tượng vòng lặp tuần hoàn JSON (Circular Dependency).
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 2 (TH2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchResponseDTO {

    private Long id;
    private String name;
    private String type;
    private BigDecimal pricePerHour;
    private String address;
    private String status;
    private String description;
    private boolean available;

    @Builder.Default
    private List<GoldenHourPriceDTO> goldenHourPrices = new ArrayList<>();
}

