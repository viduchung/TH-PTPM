package com.football.data.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ngoại lệ khi dịch vụ đối tác hoặc CSDL không thể kết nối (HTTP 503 Service Unavailable)
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(String.format("Dịch vụ '%s' hiện không khả dụng hoặc mất kết nối", serviceName), cause);
    }
}

