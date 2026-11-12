package com.football.pitchservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final String internalServiceKey;

    public JwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            @Value("${app.internal.service-key}") String internalServiceKey) {
        this.tokenProvider = tokenProvider;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Kiểm tra khóa nội bộ liên service (TH6 Machine-to-Machine)
        String internalKeyHeader = request.getHeader("X-Internal-Service-Key");
        if (StringUtils.hasText(internalKeyHeader) && internalKeyHeader.equals(internalServiceKey)) {
            List<SimpleGrantedAuthority> internalAuthorities = List.of(
                    new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_OWNER")
            );
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "internal-service", null, internalAuthorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
            return;
        }

        // 2. Kiểm tra Token JWT từ Header Authorization (TH5)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);

            if (tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsername(jwt);
                String rawRole = tokenProvider.getRole(jwt); // Ví dụ: ROLE_ADMIN, ROLE_FIELD_OWNER, ROLE_CUSTOMER

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (StringUtils.hasText(rawRole)) {
                    // Chuẩn hóa role có tiền tố ROLE_
                    String roleWithPrefix = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
                    authorities.add(new SimpleGrantedAuthority(roleWithPrefix));

                    // Nếu là ROLE_FIELD_OWNER thì map thêm ROLE_OWNER để tương thích linh hoạt
                    if ("ROLE_FIELD_OWNER".equalsIgnoreCase(roleWithPrefix)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}

