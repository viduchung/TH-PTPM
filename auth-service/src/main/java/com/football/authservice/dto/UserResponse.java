package com.football.authservice.dto;

import com.football.authservice.model.User;

import java.time.LocalDateTime;

/**
 * Dữ liệu trả ra của User - KHÔNG bao giờ lộ password_hash (TH3/TH5).
 */
public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private String fullName;
    private String phone;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.role = u.getRole() != null ? u.getRole().name() : null;
        r.fullName = u.getFullName();
        r.phone = u.getPhone();
        r.active = u.isActive();
        r.createdAt = u.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
