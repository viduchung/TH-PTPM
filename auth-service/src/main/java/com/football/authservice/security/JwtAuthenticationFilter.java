package com.football.authservice.security;

import com.football.authservice.model.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter xác thực mỗi request (TH5 + TH6).
 *  1. Nếu có header nội bộ X-Internal-Service-Key đúng -> cấp ROLE_INTERNAL_SERVICE (token propagation M2M).
 *  2. Ngược lại, đọc Bearer Token, xác thực chữ ký và nạp danh tính + vai trò vào SecurityContext.
 * Token sai/thiếu -> không set context, để Spring Security trả 401 ở lớp sau.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

    private final JwtTokenProvider tokenProvider;
    private final String internalServiceKey;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   @Value("${app.internal.service-key}") String internalServiceKey) {
        this.tokenProvider = tokenProvider;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // (TH6) Gọi liên dịch vụ bằng khóa nội bộ
        String internalKey = request.getHeader(INTERNAL_KEY_HEADER);
        if (StringUtils.hasText(internalKey) && internalKey.equals(internalServiceKey)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "internal-service", null,
                    List.of(new SimpleGrantedAuthority(Role.ROLE_INTERNAL_SERVICE.name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // (TH5) Xác thực JWT người dùng
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && tokenProvider.isValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parseClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            var authentication = new UsernamePasswordAuthenticationToken(
                    username, null,
                    List.of(new SimpleGrantedAuthority(role)));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
