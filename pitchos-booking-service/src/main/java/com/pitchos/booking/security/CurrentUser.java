package com.pitchos.booking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Người dùng hiện tại, lấy HOÀN TOÀN từ JWT đã kiểm tra chữ ký (không tin userId/role do client gửi).
 * Role theo đúng hợp đồng Auth Service: admin | chusan | user.
 */
public record CurrentUser(Long userId, String role) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_OWNER = "chusan";
    public static final String ROLE_ADMIN = "admin";
    private static final String AUTHORITY_PREFIX = "ROLE_";

    public static CurrentUser from(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(AUTHORITY_PREFIX))
                .map(authority -> authority.substring(AUTHORITY_PREFIX.length()))
                .findFirst()
                .orElse("");
        return new CurrentUser(userId, role);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isOwner() {
        return ROLE_OWNER.equals(role);
    }

    public boolean isCustomer() {
        return ROLE_USER.equals(role);
    }
}
