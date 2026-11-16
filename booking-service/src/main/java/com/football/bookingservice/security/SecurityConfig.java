package com.football.bookingservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.bookingservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * TH5: API stateless bằng JWT (không session, không cookie nên tắt CSRF).
 * Quyền chi tiết theo vai trò đặt bằng @PreAuthorize ở BookingController và kiểm tra quyền sở hữu ở service.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_DOCS = {"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"};
    private static final String[] PUBLIC_BOOKING_READS = {
            "/api/v1/bookings/quote", "/api/v1/bookings/instance-info", "/api/v1/bookings/pitches/*/schedule"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_DOCS).permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_BOOKING_READS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeError(objectMapper, request, response,
                                HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc token không hợp lệ", "ERR_UNAUTHORIZED"))
                        .accessDeniedHandler((request, response, e) -> writeError(objectMapper, request, response,
                                HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", "ERR_FORBIDDEN")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Filter là @Component nên Spring Boot cũng tự gắn vào chuỗi servlet; chỉ cho nó chạy trong chuỗi Security. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableServletRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private static void writeError(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message, String errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), errorCode));
    }
}
