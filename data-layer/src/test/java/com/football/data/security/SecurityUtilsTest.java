package com.football.data.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TH5 Unit Test: Tiện ích SecurityUtils truy xuất định danh người dùng")
public class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TH5 Test 1: Khi chưa xác thực, các phương thức trả về rỗng an toàn (Null-Safe)")
    void testUnauthenticated_ReturnsEmptySafely() {
        SecurityContextHolder.clearContext();

        assertFalse(SecurityUtils.isAuthenticated());
        assertTrue(SecurityUtils.getCurrentUserPrincipal().isEmpty());
        assertTrue(SecurityUtils.getCurrentUserId().isEmpty());
        assertTrue(SecurityUtils.getCurrentUsername().isEmpty());
        assertTrue(SecurityUtils.getCurrentAuthorities().isEmpty());
        assertFalse(SecurityUtils.hasRole("ROLE_ADMIN"));
        assertFalse(SecurityUtils.hasRole("ADMIN"));
        assertFalse(SecurityUtils.hasAnyRole("CUSTOMER", "ADMIN"));
    }

    @Test
    @DisplayName("TH5 Test 2: Khi đã xác thực, trích xuất chính xác User ID, Username và Role")
    void testAuthenticated_ExtractsInformationCorrectly() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(999L)
                .username("hung_security_lead")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_FIELD_OWNER")
                ))
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtils.isAuthenticated());
        assertTrue(SecurityUtils.getCurrentUserPrincipal().isPresent());
        assertEquals(999L, SecurityUtils.getCurrentUserId().orElse(null));
        assertEquals("hung_security_lead", SecurityUtils.getCurrentUsername().orElse(null));

        // Kiểm tra phân quyền linh hoạt (có tiền tố ROLE_ hoặc không có)
        assertTrue(SecurityUtils.hasRole("ROLE_ADMIN"));
        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertTrue(SecurityUtils.hasRole("FIELD_OWNER"));
        assertFalse(SecurityUtils.hasRole("CUSTOMER"));

        assertTrue(SecurityUtils.hasAnyRole("CUSTOMER", "ADMIN"));
        assertFalse(SecurityUtils.hasAnyRole("CUSTOMER", "GUEST"));
    }
}

