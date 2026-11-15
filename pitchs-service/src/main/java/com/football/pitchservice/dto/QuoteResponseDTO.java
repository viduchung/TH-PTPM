package com.football.pitchservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class QuoteResponseDTO {

    private Long pitchId;
    private String pitchName;
    private String pitchType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private long totalMinutes;
    private BigDecimal basePricePerHour;
    private long regularMinutes;
    private long goldenHourMinutes;
    private BigDecimal goldenHourPricePerHour;
    private BigDecimal totalAmount;
    private boolean goldenHourApplied;
    private String currency;
    private String calculationBreakdown;

    public QuoteResponseDTO() {
    }

    public QuoteResponseDTO(Long pitchId, String pitchName, String pitchType, LocalDate bookingDate,
                            LocalTime startTime, LocalTime endTime, long totalMinutes,
                            BigDecimal basePricePerHour, long regularMinutes, long goldenHourMinutes,
                            BigDecimal goldenHourPricePerHour, BigDecimal totalAmount,
                            boolean goldenHourApplied, String currency, String calculationBreakdown) {
        this.pitchId = pitchId;
        this.pitchName = pitchName;
        this.pitchType = pitchType;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalMinutes = totalMinutes;
        this.basePricePerHour = basePricePerHour;
        this.regularMinutes = regularMinutes;
        this.goldenHourMinutes = goldenHourMinutes;
        this.goldenHourPricePerHour = goldenHourPricePerHour;
        this.totalAmount = totalAmount;
        this.goldenHourApplied = goldenHourApplied;
        this.currency = currency;
        this.calculationBreakdown = calculationBreakdown;
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

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
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

    public long getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(long totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public BigDecimal getBasePricePerHour() {
        return basePricePerHour;
    }

    public void setBasePricePerHour(BigDecimal basePricePerHour) {
        this.basePricePerHour = basePricePerHour;
    }

    public long getRegularMinutes() {
        return regularMinutes;
    }

    public void setRegularMinutes(long regularMinutes) {
        this.regularMinutes = regularMinutes;
    }

    public long getGoldenHourMinutes() {
        return goldenHourMinutes;
    }

    public void setGoldenHourMinutes(long goldenHourMinutes) {
        this.goldenHourMinutes = goldenHourMinutes;
    }

    public BigDecimal getGoldenHourPricePerHour() {
        return goldenHourPricePerHour;
    }

    public void setGoldenHourPricePerHour(BigDecimal goldenHourPricePerHour) {
        this.goldenHourPricePerHour = goldenHourPricePerHour;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public boolean isGoldenHourApplied() {
        return goldenHourApplied;
    }

    public void setGoldenHourApplied(boolean goldenHourApplied) {
        this.goldenHourApplied = goldenHourApplied;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCalculationBreakdown() {
        return calculationBreakdown;
    }

    public void setCalculationBreakdown(String calculationBreakdown) {
        this.calculationBreakdown = calculationBreakdown;
    }
}
