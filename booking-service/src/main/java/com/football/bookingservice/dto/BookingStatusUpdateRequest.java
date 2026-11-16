package com.football.bookingservice.dto;

import com.football.bookingservice.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Chủ sân / admin duyệt (DA_DUYET) hoặc từ chối (TU_CHOI) đơn. */
public record BookingStatusUpdateRequest(
        @Schema(example = "DA_DUYET", allowableValues = {"DA_DUYET", "TU_CHOI"})
        @NotNull(message = "status không được để trống")
        BookingStatus status) {
}
