package com.football.pitchservice.dto;

import com.football.pitchservice.entity.Pitch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchRequestDTO {

    @NotBlank(message = "Tên sân bóng không được để trống")
    @Size(min = 3, max = 100, message = "Tên sân bóng phải từ 3 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Loại sân không được để trống")
    @Pattern(regexp = "^(SAN_5|SAN_7|SAN_11)$", message = "Loại sân phải là một trong các loại: SAN_5, SAN_7, SAN_11")
    private String type;

    @NotNull(message = "Giá thuê theo giờ không được để trống")
    @DecimalMin(value = "1000.0", message = "Giá thuê theo giờ tối thiểu là 1.000 VNĐ")
    private BigDecimal pricePerHour;

    private String address;

    @Pattern(regexp = "^(AVAILABLE|BOOKED|MAINTENANCE)$", message = "Trạng thái sân phải là: AVAILABLE, BOOKED hoặc MAINTENANCE")
    private String status; // AVAILABLE, BOOKED, MAINTENANCE

    private String description;

    @Valid
    private List<GoldenHourPriceRequestDTO> goldenHourPrices = new ArrayList<>();

    public PitchRequestDTO() {
        this.goldenHourPrices = new ArrayList<>();
    }

    public PitchRequestDTO(String name, String type, BigDecimal pricePerHour, String address, String status, String description) {
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.goldenHourPrices = new ArrayList<>();
    }

    public PitchRequestDTO(String name, String type, BigDecimal pricePerHour, String address, String status, String description, List<GoldenHourPriceRequestDTO> goldenHourPrices) {
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.goldenHourPrices = goldenHourPrices != null ? goldenHourPrices : new ArrayList<>();
    }

    public Pitch toEntity() {
        Pitch pitch = new Pitch(
                this.name,
                this.type,
                this.pricePerHour,
                this.address,
                this.status != null && !this.status.isBlank() ? this.status : "AVAILABLE",
                this.description
        );

        if (this.goldenHourPrices != null && !this.goldenHourPrices.isEmpty()) {
            this.goldenHourPrices.forEach(ghpDto -> pitch.addGoldenHourPrice(ghpDto.toEntity(pitch)));
        }

        return pitch;
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

    public List<GoldenHourPriceRequestDTO> getGoldenHourPrices() {
        return goldenHourPrices;
    }

    public void setGoldenHourPrices(List<GoldenHourPriceRequestDTO> goldenHourPrices) {
        this.goldenHourPrices = goldenHourPrices;
    }
}
