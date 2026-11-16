package com.football.bookingservice.exception;

import org.springframework.http.HttpStatus;

/** 409: trùng lịch, vi phạm luồng trạng thái, Idempotency-Key bị dùng lại cho yêu cầu khác. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "ERR_RESOURCE_CONFLICT", message);
    }
}
