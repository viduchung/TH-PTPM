package com.football.data.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ngoại lệ khi không tìm thấy tài nguyên (HTTP 404 Not Found)
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Không tìm thấy %s với %s = '%s'", resourceName, fieldName, fieldValue));
    }
}

