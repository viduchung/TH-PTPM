package com.football.bookingservice.security;

import java.util.Locale;
import java.util.Optional;

/**
 * Vai trò mà booking-service hiểu. Chuẩn hóa nhiều cách viết của các service trong nhóm:
 * auth-service phát "ROLE_FIELD_OWNER", data-layer / pitchs-service dùng "OWNER"...
 */
public enum AppRole {

    CUSTOMER,
    OWNER,
    ADMIN,
    INTERNAL_SERVICE;

    private static final String SPRING_PREFIX = "ROLE_";

    public String authority() {
        return SPRING_PREFIX + name();
    }

    /** Vai trò không nhận ra -> rỗng: người dùng đó không có quyền gì (fail-closed), không bao giờ coi là ADMIN. */
    public static Optional<AppRole> fromClaim(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith(SPRING_PREFIX)) {
            normalized = normalized.substring(SPRING_PREFIX.length());
        }
        return switch (normalized) {
            case "CUSTOMER", "USER" -> Optional.of(CUSTOMER);
            case "OWNER", "FIELD_OWNER" -> Optional.of(OWNER);
            case "ADMIN" -> Optional.of(ADMIN);
            case "INTERNAL_SERVICE" -> Optional.of(INTERNAL_SERVICE);
            default -> Optional.empty();
        };
    }
}
