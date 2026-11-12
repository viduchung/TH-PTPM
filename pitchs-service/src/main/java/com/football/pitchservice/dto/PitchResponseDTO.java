package com.football.pitchservice.dto;

import com.football.pitchservice.entity.Pitch;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<GoldenHourPriceResponseDTO> goldenHourPrices = new ArrayList<>();

    public PitchResponseDTO() {
        this.goldenHourPrices = new ArrayList<>();
    }

    public PitchResponseDTO(Long id, String name, String type, BigDecimal pricePerHour, String address, String status, String description, boolean available, List<GoldenHourPriceResponseDTO> goldenHourPrices) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.available = available;
        this.goldenHourPrices = goldenHourPrices != null ? goldenHourPrices : new ArrayList<>();
    }

    // Chuyển đổi từ JPA Entity sang DTO để tránh lộ thực thể CSDL ra ngoài
    public static PitchResponseDTO fromEntity(Pitch pitch) {
        if (pitch == null) {
            return null;
        }

        List<GoldenHourPriceResponseDTO> ghpList = new ArrayList<>();
        if (pitch.getGoldenHourPrices() != null) {
            ghpList = pitch.getGoldenHourPrices().stream()
                    .map(GoldenHourPriceResponseDTO::fromEntity)
                    .collect(Collectors.toList());
        }

        return new PitchResponseDTO(
                pitch.getId(),
                pitch.getName(),
                pitch.getType(),
                pitch.getPricePerHour(),
                pitch.getAddress(),
                pitch.getStatus(),
                pitch.getDescription(),
                "AVAILABLE".equalsIgnoreCase(pitch.getStatus()),
                ghpList
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

    public List<GoldenHourPriceResponseDTO> getGoldenHourPrices() {
        return goldenHourPrices;
    }

    public void setGoldenHourPrices(List<GoldenHourPriceResponseDTO> goldenHourPrices) {
        this.goldenHourPrices = goldenHourPrices;
    }
}
