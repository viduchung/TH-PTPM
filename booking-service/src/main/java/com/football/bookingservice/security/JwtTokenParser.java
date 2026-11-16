package com.football.bookingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Xác thực chữ ký HS256 và đọc claims bằng khóa dùng chung với auth-service (TH5).
 * Chấp nhận hai dạng token trong nhóm:
 *  - auth-service: sub = username, role = "ROLE_CUSTOMER" (chuỗi), fullName;
 *  - data-layer:   sub = username, userId = số, roles = ["ROLE_CUSTOMER"].
 */
@Slf4j
@Component
public class JwtTokenParser {

    private final SecretKey key;

    public JwtTokenParser(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Token sai chữ ký, hết hạn, sai định dạng hoặc thiếu sub -> rỗng (request bị coi là chưa đăng nhập). */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            AppRole role = resolveRole(claims).orElse(null);
            return Optional.of(new AuthenticatedUser(username, resolveUserId(claims), role, token));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT không hợp lệ: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static Optional<AppRole> resolveRole(Claims claims) {
        Object single = claims.get("role");
        if (single instanceof String role) {
            return AppRole.fromClaim(role);
        }
        if (claims.get("roles") instanceof List<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(AppRole::fromClaim)
                    .flatMap(Optional::stream)
                    .findFirst();
        }
        return Optional.empty();
    }

    private static Long resolveUserId(Claims claims) {
        Object raw = claims.get("userId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text && text.matches("\\d{1,18}")) {
            return Long.parseLong(text);
        }
        return null;
    }
}
