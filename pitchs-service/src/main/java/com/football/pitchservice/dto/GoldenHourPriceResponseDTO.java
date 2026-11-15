package com.football.pitchservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.football.pitchservice.entity.GoldenHourPrice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldenHourPriceResponseDTO {

    private Long id;
    private Long pitchId;
    private String dayOfWeek;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private BigDecimal pricePerHour;
    private String description;

    public GoldenHourPriceResponseDTO() {
    }

    public GoldenHourPriceResponseDTO(Long id, Long pitchId, String dayOfWeek, LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour, String description) {
        this.id = id;
        this.pitchId = pitchId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricePerHour = pricePerHour;
        this.description = description;
    }

    public static GoldenHourPriceResponseDTO fromEntity(GoldenHourPrice ghp) {
        if (ghp == null) return null;
        return new GoldenHourPriceResponseDTO(
                ghp.getId(),
                ghp.getPitch() != null ? ghp.getPitch().getId() : null,
                ghp.getDayOfWeek(),
                ghp.getStartTime(),
                ghp.getEndTime(),
                ghp.getPricePerHour(),
                ghp.getDescription()
        );
    }

    // Explicit Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPitchId() {
        return pitchId;
    }

    public void setPitchId(Long pitchId) {
        this.pitchId = pitchId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

