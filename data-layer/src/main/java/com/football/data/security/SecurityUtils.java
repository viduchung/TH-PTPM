package com.football.data.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Tiện ích truy xuất thông tin người dùng đang đăng nhập từ SecurityContext.
 * Cung cấp API trực quan, an toàn luồng (Thread-safe) cho các tầng Controller,
 * Service của tất cả các microservices (booking-service, pitchs-service, payment-service).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class private constructor
    }

    /**
     * Lấy Authentication hiện tại từ SecurityContextHolder
     */
    public static Optional<Authentication> getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return Optional.empty();
        }
        Authentication authentication = context.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getPrincipal().toString())) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    /**
     * Lấy UserPrincipal của người dùng hiện tại
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserPrincipal)
                .map(principal -> (UserPrincipal) principal);
    }

    /**
     * Lấy ID của người dùng hiện tại (Dùng cho booking-service gán userId vào đơn đặt sân)
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getId);
    }

    /**
     * Lấy Username của người dùng hiện tại
     */
    public static Optional<String> getCurrentUsername() {
        return getAuthentication().map(Authentication::getName);
    }

    /**
     * Kiểm tra xem request hiện tại đã được xác thực hay chưa
     */
    public static boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    /**
     * Lấy danh sách quyền hạn (Authorities) của người dùng hiện tại
     */
    public static Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getAuthentication()
                .map(Authentication::getAuthorities)
                .orElse(Collections.emptyList());
    }

    /**
     * Kiểm tra xem người dùng hiện tại có vai trò cụ thể hay không
     * Ví dụ: hasRole("ROLE_ADMIN") hoặc hasRole("ADMIN")
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equalsIgnoreCase(roleWithPrefix));
    }

    /**
     * Kiểm tra xem người dùng hiện tại có bất kỳ vai trò nào trong danh sách hay không
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}

