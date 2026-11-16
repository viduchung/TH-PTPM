package com.football.bookingservice.exception;

import org.springframework.http.HttpStatus;

/** 503: service phụ thuộc (pitchs-service, auth-service) không phản hồi hoặc trả dữ liệu không dùng được. */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "ERR_SERVICE_DOWN", message, cause);
    }

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "ERR_SERVICE_DOWN", message);
    }
}
