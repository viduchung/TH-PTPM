package com.football.pitchservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép tất cả xem danh sách, tìm kiếm, xem chi tiết và lấy báo giá /quote
                        .requestMatchers(HttpMethod.GET, "/api/pitches/**", "/api/pitchs/**", "/api/v1/pitches/**").permitAll()

                        // 2. Tài liệu Swagger UI và H2 console
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**").permitAll()

                        // 3. Các thao tác Thêm, Sửa, Xóa sân & giờ vàng bắt buộc phải có vai trò OWNER hoặc ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/pitches/**", "/api/pitchs/**", "/api/v1/pitches/**").hasAnyRole("OWNER", "FIELD_OWNER", "ADMIN", "INTERNAL_SERVICE")
                        .requestMatchers(HttpMethod.PUT, "/api/pitches/**", "/api/pitchs/**", "/api/v1/pitches/**").hasAnyRole("OWNER", "FIELD_OWNER", "ADMIN", "INTERNAL_SERVICE")
                        .requestMatchers(HttpMethod.DELETE, "/api/pitches/**", "/api/pitchs/**", "/api/v1/pitches/**").hasAnyRole("OWNER", "FIELD_OWNER", "ADMIN", "INTERNAL_SERVICE")

                        // 4. Các request còn lại yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Hỗ trợ hiển thị H2 console trong iframe dev
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}

