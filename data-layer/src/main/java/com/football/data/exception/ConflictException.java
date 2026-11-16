package com.football.data.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ngoại lệ khi xảy ra xung đột dữ liệu tài nguyên (HTTP 409 Conflict)
 * Ví dụ: Trùng lịch sân bóng đá hoặc tên đăng nhập đã được sử dụng.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

