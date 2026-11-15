package com.football.data.security;

import com.football.data.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lớp tiện ích quản lý phát hành, giải mã và xác thực chữ ký số JSON Web Token (JWT).
 * Tương thích chuẩn JJWT 0.12.x với thuật toán mã hóa HMAC-SHA256.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Lấy SecretKey an toàn cho thuật toán ký số HMAC-SHA256
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Sinh JWT Token từ UserPrincipal
     */
    public String generateToken(UserPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return generateToken(principal.getId(), principal.getUsername(), roles, jwtProperties.getExpirationMs());
    }

    /**
     * Sinh JWT Token từ User ID, Username và Role đơn lẻ
     */
    public String generateToken(Long userId, String username, Role role) {
        return generateToken(userId, username, Collections.singletonList(role.name()), jwtProperties.getExpirationMs());
    }

    /**
     * Sinh JWT Token từ User ID, Username và danh sách Roles
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        return generateToken(userId, username, roles, jwtProperties.getExpirationMs());
    }

    /**
     * Sinh JWT Token với thời gian hết hạn tùy biến (hỗ trợ kiểm thử và refresh token)
     */
    public String generateToken(Long userId, String username, List<String> roles, long customExpirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Trích xuất toàn bộ Claims từ token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Trích xuất Username (Subject) từ token
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Trích xuất User ID từ token (Xử lý an toàn tránh lỗi cast Number sang Long)
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object rawUserId = claims.get("userId");
        if (rawUserId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Trích xuất danh sách Roles từ token
     */
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rawRoles = claims.get("roles");
        if (rawRoles instanceof List<?> list) {
            List<String> roles = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            return roles;
        }
        return Collections.emptyList();
    }

    /**
     * Trích xuất thời điểm hết hạn của token
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Kiểm tra token đã hết hạn hay chưa
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Xác thực chữ ký và cấu trúc của JWT Token
     * 
     * @param token Chuỗi JWT token
     * @return true nếu token hợp lệ và còn hạn; false nếu giả mạo, sai cấu trúc hoặc hết hạn
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Chữ ký JWT không hợp lệ hoặc bị giả mạo: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT đã hết hạn sử dụng: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Định dạng JWT không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Chuỗi JWT claims rỗng hoặc không hợp lệ: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Lỗi không xác định khi xác thực JWT: {}", e.getMessage());
        }
        return false;
    }
}
