package com.football.bookingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/**
 * Mỗi request:
 *  1. Header X-Internal-Service-Key đúng -> ROLE_INTERNAL_SERVICE (service khác gọi sang, TH6).
 *  2. Ngược lại đọc "Authorization: Bearer ..." -> nạp AuthenticatedUser vào SecurityContext (TH5).
 * Token sai / thiếu -> không set gì, Spring Security trả 401 ở endpoint cần đăng nhập.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser tokenParser;
    private final byte[] internalServiceKey;

    public JwtAuthenticationFilter(JwtTokenParser tokenParser,
                                   @Value("${app.internal.service-key}") String internalServiceKey) {
        this.tokenParser = tokenParser;
        this.internalServiceKey = internalServiceKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveUser(request).ifPresent(JwtAuthenticationFilter::authenticate);
        }
        chain.doFilter(request, response);
    }

    private Optional<AuthenticatedUser> resolveUser(HttpServletRequest request) {
        String internalKey = request.getHeader(INTERNAL_KEY_HEADER);
        if (internalKey != null && isInternalKeyValid(internalKey)) {
            return Optional.of(AuthenticatedUser.internalService());
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return tokenParser.parse(header.substring(BEARER_PREFIX.length()).trim());
    }

    /** So sánh thời gian hằng định, tránh dò khóa qua thời gian phản hồi. */
    private boolean isInternalKeyValid(String candidate) {
        return MessageDigest.isEqual(internalServiceKey, candidate.getBytes(StandardCharsets.UTF_8));
    }

    private static void authenticate(AuthenticatedUser user) {
        List<SimpleGrantedAuthority> authorities = user.role() == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(user.role().authority()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
