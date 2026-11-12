package com.football.pitchservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.football.pitchservice.entity.GoldenHourPrice;
import com.football.pitchservice.entity.Pitch;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldenHourPriceRequestDTO {

    @NotBlank(message = "Ngày áp dụng không được để trống (ví dụ: ALL_DAYS, WEEKEND, MONDAY)")
    private String dayOfWeek;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotNull(message = "Giá giờ vàng không được để trống")
    @DecimalMin(value = "1000.0", message = "Giá giờ vàng tối thiểu là 1.000 VNĐ")
    private BigDecimal pricePerHour;

    private String description;

    public GoldenHourPrice toEntity(Pitch pitch) {
        return new GoldenHourPrice(
                pitch,
                this.dayOfWeek,
                this.startTime,
                this.endTime,
                this.pricePerHour,
                this.description
        );
    }

    // Explicit Getters and Setters
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

