package com.football.pitchservice.dto;

import com.football.pitchservice.entity.Pitch;
import lombok.*;

import java.math.BigDecimal;

@Data
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

    public PitchResponseDTO() {
    }

    public PitchResponseDTO(Long id, String name, String type, BigDecimal pricePerHour, String address, String status, String description, boolean available) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.available = available;
    }

    // Chuyển đổi từ JPA Entity sang DTO để tránh lộ thực thể CSDL ra ngoài
    public static PitchResponseDTO fromEntity(Pitch pitch) {
        if (pitch == null) {
            return null;
        }
        return new PitchResponseDTO(
                pitch.getId(),
                pitch.getName(),
                pitch.getType(),
                pitch.getPricePerHour(),
                pitch.getAddress(),
                pitch.getStatus(),
                pitch.getDescription(),
                "AVAILABLE".equalsIgnoreCase(pitch.getStatus())
        );
    }

    // Explicit Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
