package com.football.data.security;

import com.football.data.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TH5 Unit Test: Tiện ích xử lý và xác thực JSON Web Token (JwtTokenProvider)")
public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setExpirationMs(3600000L); // 1 giờ
        jwtProperties.setIssuer("football-test-platform");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("TH5 Test 1: Sinh token từ User ID, Username, Role và giải mã chính xác")
    void testGenerateAndParseToken_Success() {
        Long userId = 101L;
        String username = "hung_db";
        Role role = Role.ROLE_ADMIN;

        String token = jwtTokenProvider.generateToken(userId, username, role);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(username, jwtTokenProvider.extractUsername(token));
        assertEquals(userId, jwtTokenProvider.extractUserId(token));

        List<String> roles = jwtTokenProvider.extractRoles(token);
        assertEquals(1, roles.size());
        assertEquals("ROLE_ADMIN", roles.get(0));
        assertFalse(jwtTokenProvider.isTokenExpired(token));
    }

    @Test
    @DisplayName("TH5 Test 2: Sinh token từ UserPrincipal đa vai trò")
    void testGenerateTokenFromPrincipal() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(202L)
                .username("hoang_auth")
                .fullName("Lê Huy Hoàng")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_FIELD_OWNER"),
                        new SimpleGrantedAuthority("ROLE_CUSTOMER")
                ))
                .build();

        String token = jwtTokenProvider.generateToken(principal);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(202L, jwtTokenProvider.extractUserId(token));
        assertEquals("hoang_auth", jwtTokenProvider.extractUsername(token));

        List<String> roles = jwtTokenProvider.extractRoles(token);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_FIELD_OWNER"));
        assertTrue(roles.contains("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("TH5 Test 3: Token hết hạn (Expired Token) bị từ chối validate")
    void testExpiredToken_ValidationFails() {
        // Sinh token có thời hạn âm (-5 giây)
        String expiredToken = jwtTokenProvider.generateToken(
                303L, "expired_user", List.of("ROLE_CUSTOMER"), -5000L
        );

        assertNotNull(expiredToken);
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
        assertTrue(jwtTokenProvider.isTokenExpired(expiredToken));
    }

    @Test
    @DisplayName("TH5 Test 4: Token bị sửa đổi chữ ký (Tampered Token) bị từ chối")
    void testTamperedToken_ValidationFails() {
        String token = jwtTokenProvider.generateToken(404L, "valid_user", Role.ROLE_CUSTOMER);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        // Làm sai lệch chữ ký số (Signature Mismatch)
        String tamperedToken = parts[0] + "." + parts[1] + "." + "InvalidSignature1234567890abcdef";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("TH5 Test 5: Token rỗng, null hoặc định dạng vô lệ trả về false")
    void testInvalidTokenStrings_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.token.structure"));
        assertFalse(jwtTokenProvider.validateToken("Bearer dummy_token"));
    }
}

