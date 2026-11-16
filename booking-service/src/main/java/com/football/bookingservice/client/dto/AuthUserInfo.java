package com.football.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Phần booking cần từ UserResponse của auth-service (GET /api/v1/auth/me). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserInfo(Long id, String username, String role, boolean active) {
}
