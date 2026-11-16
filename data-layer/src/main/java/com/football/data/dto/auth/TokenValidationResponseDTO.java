package com.football.data.dto.auth;

import lombok.*;

/**
 * Kết quả xác thực Token JWT giữa các dịch vụ (TH2 + TH6)
 * Giúp Booking Service, Pitch Service hoặc API Gateway xác định danh tính và vai trò
 * của người dùng mà không cần phải truy cập trực tiếp vào CSDL auth_db.
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 2 (TH2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponseDTO {

    private boolean valid;
    private Long userId;
    private String username;
    private String role;
    private String fullName;
    private String message;

    public static TokenValidationResponseDTO valid(Long userId, String username, String role, String fullName) {
        return TokenValidationResponseDTO.builder()
                .valid(true)
                .userId(userId)
                .username(username)
                .role(role)
                .fullName(fullName)
                .message("Token hợp lệ")
                .build();
    }

    public static TokenValidationResponseDTO invalid(String message) {
        return TokenValidationResponseDTO.builder()
                .valid(false)
                .message(message)
                .build();
    }
}

