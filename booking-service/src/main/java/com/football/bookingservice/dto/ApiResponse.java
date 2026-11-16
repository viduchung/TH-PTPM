package com.football.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/** Vỏ phản hồi thành công, cùng cấu trúc ApiResponse của data-layer (Hùng, TH3). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }
}
