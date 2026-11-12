package com.football.pitchservice.dto;

import com.football.pitchservice.entity.Pitch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchRequestDTO {

    @NotBlank(message = "Tên sân bóng không được để trống")
    private String name;

    @NotBlank(message = "Loại sân không được để trống (ví dụ: SAN_5, SAN_7, SAN_11)")
    private String type;

    @NotNull(message = "Giá thuê theo giờ không được để trống")
    @Positive(message = "Giá thuê theo giờ phải lớn hơn 0")
    private BigDecimal pricePerHour;

    private String address;

    private String status; // AVAILABLE, BOOKED, MAINTENANCE

    private String description;

    public PitchRequestDTO() {
    }

    public PitchRequestDTO(String name, String type, BigDecimal pricePerHour, String address, String status, String description) {
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
    }

    public Pitch toEntity() {
        return new Pitch(
                this.name,
                this.type,
                this.pricePerHour,
                this.address,
                this.status != null && !this.status.isBlank() ? this.status : "AVAILABLE",
                this.description
        );
    }

    // Explicit Getters and Setters
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
}
