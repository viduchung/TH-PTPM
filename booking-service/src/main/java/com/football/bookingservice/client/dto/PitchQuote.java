package com.football.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Phần booking cần từ QuoteResponseDTO của pitchs-service (GET /api/v1/pitches/{id}/quote, TH6 Provider). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PitchQuote(Long pitchId, String pitchName, long totalMinutes, BigDecimal basePricePerHour,
                         BigDecimal totalAmount, boolean goldenHourApplied, String calculationBreakdown) {
}
