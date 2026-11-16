package com.football.bookingservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "ERR_RESOURCE_NOT_FOUND", message);
    }
}
