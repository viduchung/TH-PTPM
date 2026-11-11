package com.football.authservice.exception;

/** Ngoại lệ 400 - vi phạm quy tắc nghiệp vụ (VD: username đã tồn tại). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
