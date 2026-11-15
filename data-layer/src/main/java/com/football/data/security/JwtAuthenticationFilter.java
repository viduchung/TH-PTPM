package com.football.data.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bộ lọc xác thực JWT (Filter) thực thi một lần cho mỗi HTTP Request.
 * Trích xuất Bearer Token từ header 'Authorization', kiểm tra chữ ký số,
 * và nạp đối tượng UserPrincipal vào SecurityContextHolder.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.extractUsername(jwt);
                Long userId = jwtTokenProvider.extractUserId(jwt);
                List<String> roles = jwtTokenProvider.extractRoles(jwt);

                // Tái tạo UserPrincipal từ Claims của JWT mà không cần gọi auth_db
                UserPrincipal principal = UserPrincipal.fromClaims(userId, username, roles);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Nạp thông tin định danh vào SecurityContext để các tầng service/controller trích xuất
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Đã xác thực thành công người dùng: {} (ID: {}) với vai trò: {}", username, userId, roles);
            }
        } catch (Exception e) {
            log.error("Không thể thiết lập xác thực người dùng trong Security Context: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trích xuất Bearer token từ Authorization Header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeaderString());
        String prefix = jwtProperties.getTokenPrefix();

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length());
        }
        return null;
    }
}
