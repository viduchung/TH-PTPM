package com.football.data.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.football.data.auth.entity.Role;
import com.football.data.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Đối tượng đại diện người dùng đã xác thực (User Principal) trong Spring Security.
 * Chứa ID người dùng và thông tin phân quyền để các microservice trích xuất trực tiếp
 * mà không cần phải truy vấn lại cơ sở dữ liệu `auth_db`.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String fullName;

    private String phone;

    private String email;

    @Builder.Default
    private boolean enabled = true;

    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Tạo UserPrincipal từ thực thể User (Dùng cho auth-service khi đăng nhập/kiểm tra DB)
     */
    public static UserPrincipal fromUser(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .enabled(user.isActive())
                .authorities(authorities)
                .build();
    }

    /**
     * Tái tạo UserPrincipal từ Claims của JWT Token (Dùng cho các service: pitchs, booking, payment)
     */
    public static UserPrincipal fromClaims(Long id, String username, List<String> roles) {
        List<GrantedAuthority> authorities = roles == null ? Collections.emptyList() :
                roles.stream()
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return UserPrincipal.builder()
                .id(id)
                .username(username)
                .password(null) // Token stateless không chứa mật khẩu
                .authorities(authorities)
                .enabled(true)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Kiểm tra xem người dùng có quyền cụ thể hay không (ví dụ: "ROLE_ADMIN" hoặc "ADMIN")
     */
    public boolean hasRole(String role) {
        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        if (authorities == null) {
            return false;
        }
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(targetRole));
    }
}
