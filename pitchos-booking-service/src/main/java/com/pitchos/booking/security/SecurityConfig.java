package com.pitchos.booking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.pitchos.booking.security.CurrentUser.ROLE_ADMIN;
import static com.pitchos.booking.security.CurrentUser.ROLE_OWNER;
import static com.pitchos.booking.security.CurrentUser.ROLE_USER;

/**
 * Phân quyền Booking Service (role theo hợp đồng Auth Service: admin | chusan | user):
 *   POST   /api/v1/bookings              → user                 (giữ như cũ)
 *   GET    /api/v1/bookings/my-bookings  → user                 (giữ như cũ)
 *   PATCH  /api/v1/bookings/{id}/status  → chusan, admin        (giữ như cũ)
 *   PUT    /api/v1/bookings/{id}         → user (chủ đơn)
 *   PATCH  /api/v1/bookings/{id}/cancel  → user (chủ đơn), admin
 *   DELETE /api/v1/bookings/{id}         → user (chủ đơn), admin (hủy mềm, giữ lịch sử)
 *   GET    /api/v1/bookings, /{id}, /quote → đã đăng nhập; quyền sở hữu kiểm tra ở BookingAccessPolicy
 * Không có / sai / hết hạn token → 401; đúng token nhưng sai vai trò hoặc không sở hữu → 403. Cả hai trả JSON.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String BOOKINGS = "/api/v1/bookings";

    private final JwtUtil jwtUtil;
    private final JsonSecurityHandlers jsonSecurityHandlers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, BOOKINGS).hasRole(ROLE_USER)
                        .requestMatchers(HttpMethod.GET, BOOKINGS + "/my-bookings").hasRole(ROLE_USER)
                        .requestMatchers(HttpMethod.PATCH, BOOKINGS + "/*/status").hasAnyRole(ROLE_OWNER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, BOOKINGS + "/*/cancel").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, BOOKINGS + "/*").hasRole(ROLE_USER)
                        .requestMatchers(HttpMethod.DELETE, BOOKINGS + "/*").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonSecurityHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(jsonSecurityHandlers.accessDeniedHandler()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
