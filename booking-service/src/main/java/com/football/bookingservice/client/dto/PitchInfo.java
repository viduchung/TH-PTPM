package com.football.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Phần booking cần từ PitchResponseDTO của pitchs-service (GET /api/v1/pitches/{id}).
 * Chỉ khai báo trường cần dùng, bỏ qua trường lạ -> pitchs-service thêm trường không làm vỡ booking.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PitchInfo(Long id, String name, String type, BigDecimal pricePerHour, String status) {

    public static final String STATUS_MAINTENANCE = "MAINTENANCE";

    public boolean isUnderMaintenance() {
        return STATUS_MAINTENANCE.equalsIgnoreCase(status);
    }
}
