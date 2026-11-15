package com.pitchos.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Định dạng lỗi thống nhất trả về client, ví dụ:
 * { "timestamp": "...", "status": 503, "error": "Service Unavailable", "message": "...", "path": "/api/v1/bookings" }
 * Giữ nguyên 4 trường cũ; path và details (danh sách lỗi từng trường) chỉ xuất hiện khi có giá trị.
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details;

    public static ErrorResponse of(HttpStatus status, String message) {
        return of(status, message, null, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<String> details) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}
