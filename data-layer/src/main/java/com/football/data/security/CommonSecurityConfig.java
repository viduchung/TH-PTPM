package com.football.data.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình bảo mật dùng chung (Common Security Configuration).
 * Áp dụng mô hình Stateless JWT Authentication và Role-Based Access Control (RBAC).
 * 
 * Cung cấp:
 * 1. Tắt CSRF (vì REST API hoàn toàn stateless, không dùng session cookie).
 * 2. Bộ mã hóa mật khẩu PasswordEncoder chuẩn BCrypt (độ an toàn cao).
 * 3. Chặn và xử lý lỗi xác thực 401 Unauthorized / phân quyền 403 Forbidden theo chuẩn JSON RFC 7807.
 * 4. Chèn JwtAuthenticationFilter vào trước UsernamePasswordAuthenticationFilter.
 * 5. Hỗ trợ kích hoạt @PreAuthorize trên từng Controller method.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class CommonSecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Bean mã hóa mật khẩu một chiều BCrypt cho auth-service
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager tiêu chuẩn của Spring Security
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Chuỗi bộ lọc bảo mật SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì API sử dụng Bearer Token hoàn toàn không trạng thái (Stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Kích hoạt CORS với cấu hình mặc định
                .cors(Customizer.withDefaults())

                // Xử lý ngoại lệ chuẩn RFC 7807 trả về JSON
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // Cấu hình Session Management ở chế độ Stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Phân quyền trên các đường dẫn URL
                .authorizeHttpRequests(auth -> auth
                        // Endpoints công khai không yêu cầu token
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/api/test/**",
                                "/api/integration/**",
                                "/h2-console/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // Endpoints phân quyền theo RBAC
                        .requestMatchers("/api/secured/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/secured/field-owner/**").hasAnyRole("FIELD_OWNER", "ADMIN")
                        .requestMatchers("/api/secured/customer/**").hasAnyRole("CUSTOMER", "FIELD_OWNER", "ADMIN")
                        .requestMatchers("/api/secured/**").authenticated()

                        // Mọi yêu cầu khác đều cần được xác thực
                        .anyRequest().authenticated()
                )

                // Vô hiệu hóa frameOptions để truy cập H2-Console trong môi trường kiểm thử
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                );

        // Chèn JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

