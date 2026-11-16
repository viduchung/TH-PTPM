package com.football.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Phản hồi lỗi thống nhất (RFC 7807 rút gọn) - cùng cấu trúc ErrorResponse của data-layer. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message,
                            String path, String errorCode, Map<String, String> errors) {

    public static ErrorResponse of(int status, String error, String message, String path, String errorCode) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, errorCode, null);
    }

    public static ErrorResponse validation(String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), 400, "Validation Failed", message, path,
                "ERR_VALIDATION", fieldErrors);
    }
}
