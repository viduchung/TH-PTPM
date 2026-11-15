package com.pitchos.booking.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Thông tin sân nhận về từ Pitch Service (GET /api/v1/pitches/{id}).
 * Chỉ khai báo các trường Booking cần; trường lạ trong JSON được bỏ qua.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PitchResponseDTO {

    public static final String STATUS_ACTIVE = "hoat_dong";

    private Long id;
    private String name;
    private BigDecimal pricePerHour;
    private String status;   // hoat_dong | bao_tri

    /**
     * ID chủ sân. Pitch Service HIỆN CHƯA trả trường này (null) → xem cấu hình
     * booking.security.chusan-without-owner-info. Khi Pitch Service bổ sung, Booking tự kiểm tra quyền chủ sân.
     */
    private Long ownerId;

    public PitchResponseDTO(Long id, String name, BigDecimal pricePerHour, String status) {
        this(id, name, pricePerHour, status, null);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}
