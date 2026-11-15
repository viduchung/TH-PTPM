package com.football.pitchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "golden_hour_prices")
@Data
@Builder
public class GoldenHourPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ Many-to-One: Nhiều khung giờ vàng thuộc về một Sân bóng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private Pitch pitch;

    @Column(name = "day_of_week", nullable = false, length = 50)
    private String dayOfWeek; // ALL_DAYS, WEEKDAY, WEEKEND, MONDAY,...

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // Giờ bắt đầu (ví dụ: 17:30:00)

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // Giờ kết thúc (ví dụ: 20:30:00)

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour; // Giá trong khung giờ vàng

    @Column(length = 255)
    private String description;

    // No-arg constructor bắt buộc đối với JPA Entity
    public GoldenHourPrice() {
    }

    public GoldenHourPrice(Long id, Pitch pitch, String dayOfWeek, LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour, String description) {
        this.id = id;
        this.pitch = pitch;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricePerHour = pricePerHour;
        this.description = description;
    }

    // Constructor phục vụ việc tạo mới
    public GoldenHourPrice(Pitch pitch, String dayOfWeek, LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour, String description) {
        this.pitch = pitch;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricePerHour = pricePerHour;
        this.description = description;
    }

    public GoldenHourPrice(String dayOfWeek, LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour, String description) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricePerHour = pricePerHour;
        this.description = description;
    }

    // Explicit Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pitch getPitch() {
        return pitch;
    }

    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
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
