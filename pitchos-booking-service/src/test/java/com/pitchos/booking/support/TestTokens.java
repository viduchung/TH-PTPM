package com.pitchos.booking.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * Sinh JWT giống hệt JwtTokenProvider của Auth Service: sub = username, claim userId, role (chữ thường),
 * fullName, HS256 với jwt.secret dùng chung. Dùng để kiểm thử Booking mà không cần chạy Auth Service + MySQL.
 */
public final class TestTokens {

    /** Giá trị mặc định của jwt.secret trong .env.example / application.properties (không phải bí mật thật). */
    public static final String SHARED_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String OTHER_SECRET = "5A7134743777217A25432A462D4A614E645267556B58703273357638792F423F";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private TestTokens() {
    }

    public static String user(long userId) {
        return token(userId, "user", SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String owner(long userId) {
        return token(userId, "chusan", SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String admin(long userId) {
        return token(userId, "admin", SHARED_SECRET, ONE_HOUR_MS);
    }

    public static String expiredUser(long userId) {
        return token(userId, "user", SHARED_SECRET, -ONE_HOUR_MS);
    }

    public static String signedWithWrongKey(long userId) {
        return token(userId, "user", OTHER_SECRET, ONE_HOUR_MS);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String token(long userId, String role, String secret, long validityMs) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date now = new Date();
        return Jwts.builder()
                .setSubject(role + userId)
                .claim("userId", userId)
                .claim("role", role)
                .claim("fullName", "Test " + role)
                .setIssuedAt(new Date(now.getTime() - ONE_HOUR_MS * 2))
                .setExpiration(new Date(now.getTime() + validityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
