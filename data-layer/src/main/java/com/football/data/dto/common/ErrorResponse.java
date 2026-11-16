package com.football.data.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cấu trúc phản hồi lỗi chuẩn hóa toàn hệ sinh thái Microservices
 * Tuân thủ tiêu chuẩn RFC 7807 (Problem Details for HTTP APIs).
 * 
 * Áp dụng thống nhất cho auth-service, pitchs-service, booking-service, payment-service.
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 3 (TH3)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;

    private String error;

    private String message;

    private String path;

    private String errorCode;

    /** Danh sách chi tiết các trường bị lỗi validation (Field -> Message) */
    private Map<String, String> errors;

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    public static ErrorResponse of(int status, String error, String message, String path, String errorCode) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .errorCode(errorCode)
                .build();
    }

    public static ErrorResponse ofValidation(int status, String message, String path, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error("Validation Failed")
                .message(message)
                .path(path)
                .errorCode("ERR_VALIDATION")
                .errors(fieldErrors)
                .build();
    }
}

