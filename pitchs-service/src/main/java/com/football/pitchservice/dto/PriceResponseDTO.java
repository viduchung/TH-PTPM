package com.football.pitchservice.dto;

import com.football.pitchservice.entity.Pitch;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceResponseDTO {

    private Long pitchId;
    private String pitchName;
    private String pitchType;
    private BigDecimal pricePerHour;
    private String currency;
    private String status;
    private boolean available;

    public PriceResponseDTO() {
    }

    public PriceResponseDTO(Long pitchId, String pitchName, String pitchType, BigDecimal pricePerHour, String currency, String status, boolean available) {
        this.pitchId = pitchId;
        this.pitchName = pitchName;
        this.pitchType = pitchType;
        this.pricePerHour = pricePerHour;
        this.currency = currency;
        this.status = status;
        this.available = available;
    }

    public static PriceResponseDTO fromEntity(Pitch pitch) {
        if (pitch == null) {
            return null;
        }
        return new PriceResponseDTO(
                pitch.getId(),
                pitch.getName(),
                pitch.getType(),
                pitch.getPricePerHour(),
                "VND",
                pitch.getStatus(),
                "AVAILABLE".equalsIgnoreCase(pitch.getStatus())
        );
    }

    // Explicit Getters and Setters
    public Long getPitchId() {
        return pitchId;
    }

    public void setPitchId(Long pitchId) {
        this.pitchId = pitchId;
    }

    public String getPitchName() {
        return pitchName;
    }

    public void setPitchName(String pitchName) {
        this.pitchName = pitchName;
    }

    public String getPitchType() {
        return pitchType;
    }

    public void setPitchType(String pitchType) {
        this.pitchType = pitchType;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
