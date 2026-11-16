package com.football.authservice.service;

import com.football.authservice.dto.*;
import com.football.authservice.exception.BadRequestException;
import com.football.authservice.model.Role;
import com.football.authservice.model.User;
import com.football.authservice.repository.UserRepository;
import com.football.authservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ xác thực: đăng ký, đăng nhập, xác thực token (TH1/TH5/TH6).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /** Đăng ký tài khoản mới - băm mật khẩu bằng BCrypt (TH1 + TH5). */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Số điện thoại '" + request.getPhone() + "' đã được sử dụng");
        }

        Role role = request.getRole();
        // Mặc định là khách hàng; không cho tự đăng ký quyền nội bộ
        if (role == null || role == Role.ROLE_INTERNAL_SERVICE) {
            role = Role.ROLE_CUSTOMER;
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                role,
                request.getFullName(),
                request.getPhone());

        return UserResponse.from(userRepository.save(user));
    }

    /** Đăng nhập - so khớp BCrypt và cấp JWT (TH5). */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Tài khoản đã bị khóa");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Sai tên đăng nhập hoặc mật khẩu");
        }

        String token = tokenProvider.generateToken(
                user.getUsername(), user.getRole().name(), user.getFullName());

        return new AuthResponse(
                token,
                tokenProvider.getExpirationSeconds(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName());
    }

    /** Xác thực token cho service khác gọi sang (TH2/TH6). */
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String token) {
        try {
            Claims claims = tokenProvider.parseClaims(token);
            return TokenValidationResponse.valid(
                    claims.getSubject(),
                    claims.get("role", String.class),
                    claims.get("fullName", String.class));
        } catch (Exception ex) {
            return TokenValidationResponse.invalid("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Không tìm thấy phiên đăng nhập"));
    }
}
