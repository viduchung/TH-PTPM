package com.football.bookingservice.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Sinh JWT giống hai nguồn phát token trong nhóm:
 *  - authStyle(...)     : y hệt JwtTokenProvider của auth-service (sub, role, fullName - KHÔNG có userId);
 *  - customer/owner/... : dạng data-layer (sub, userId, roles[]) để test không cần auth-service.
 */
public final class TestTokens {

    /** Trùng app.jwt.secret mặc định của nhóm (README mục "JWT Secret Key dùng chung"). */
    public static final String SHARED_SECRET =
            "FootballBookingAuthServiceSecretKey2026_ChangeMeInProductionPlease_0123456789";
    public static final String INTERNAL_KEY = "FootballInternalSecretServiceKey2026";
    private static final String OTHER_SECRET = "SomeOtherServiceSecretKeyThatIsDefinitelyLongEnough_9876543210";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private TestTokens() {
    }

    public static String customer(long userId) {
        return dataLayerStyle(userId, "customer" + userId, "ROLE_CUSTOMER", SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String owner(long userId) {
        return dataLayerStyle(userId, "owner" + userId, "ROLE_FIELD_OWNER", SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String admin(long userId) {
        return dataLayerStyle(userId, "admin" + userId, "ROLE_ADMIN", SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String withRole(long userId, String role) {
        return dataLayerStyle(userId, "user" + userId, role, SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String expiredCustomer(long userId) {
        return dataLayerStyle(userId, "customer" + userId, "ROLE_CUSTOMER", SHARED_SECRET, -ONE_HOUR_MS);
    }

    public static String signedWithWrongKey(long userId) {
        return dataLayerStyle(userId, "customer" + userId, "ROLE_CUSTOMER", OTHER_SECRET, ONE_HOUR_MS);
    }

    /** Token đúng như auth-service phát hành sau /login. */
    public static String authStyle(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("fullName", "Test " + username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ONE_HOUR_MS))
                .signWith(key(SHARED_SECRET))
                .compact();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String dataLayerStyle(long userId, String username, String role, String secret, long validityMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .issuedAt(new Date(now.getTime() - 2 * ONE_HOUR_MS))
                .expiration(new Date(now.getTime() + validityMs))
                .signWith(key(secret))
                .compact();
    }

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
