package com.football.bookingservice.exception;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ có sẵn mã HTTP và errorCode theo chuẩn chung của nhóm (data-layer). */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected ApiException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
