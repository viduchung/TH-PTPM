package com.football.data.dto.pitch;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO Báo giá sân bóng nhanh phục vụ Booking Service (Service-to-Service Contract - TH2)
 * 
 * Khi Booking Service chuẩn bị tạo đơn đặt sân hoặc tính tiền, nó KHÔNG ĐƯỢC
 * phụ thuộc vào Entity Pitch của Pitch Service mà chỉ cần nhận DTO tinh gọn này
 * qua endpoint: GET /api/pitches/{id}/price
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 2 (TH2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceResponseDTO {

    private Long pitchId;
    private String pitchName;
    private String pitchType;
    private BigDecimal pricePerHour;
    @Builder.Default
    private String currency = "VND";
    private String status;
    private boolean available;
}

