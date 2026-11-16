package com.football.authservice.controller;

import com.football.authservice.dto.*;
import com.football.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API Xác thực (TH1/TH5/TH6).
 *  POST /api/v1/auth/register   - đăng ký
 *  POST /api/v1/auth/login      - đăng nhập, cấp JWT
 *  POST /api/v1/auth/validate   - xác thực token cho service khác (token propagation)
 *  GET  /api/v1/auth/me         - thông tin phiên hiện tại
 *  GET  /api/v1/auth/health     - kiểm tra trạng thái service
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Xác thực & cấp phát JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Đăng ký tài khoản mới")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Đăng nhập, nhận JWT Access Token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Xác thực token (dành cho service khác gọi sang)")
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "");
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @Operation(summary = "Thông tin người dùng của phiên đăng nhập hiện tại")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.getByUsername(auth.getName()));
    }

    @Operation(summary = "Kiểm tra trạng thái service")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "Auth Service");
        info.put("port", 8080);
        info.put("version", "1.0");
        info.put("status", "UP");
        return ResponseEntity.ok(info);
    }
}
