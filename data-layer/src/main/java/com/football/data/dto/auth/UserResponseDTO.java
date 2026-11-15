package com.football.data.dto.auth;

import lombok.*;

/**
 * DTO đại diện thông tin người dùng an toàn (User Profile DTO)
 * =========================================================================
 * NGUYÊN TẮC THIẾT KẾ DTO CỦA HÙNG (TRÁNH TRÙNG MODEL / SCHEMA LEAKAGE):
 * 1. Tuyệt đối KHÔNG trả về `passwordHash` để tránh rò rỉ mã băm mật khẩu.
 * 2. Tách biệt hoàn toàn khỏi Entity JPA, không mang theo Hibernate proxies.
 * =========================================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String username;
    private String role;
    private String roleDisplayName;
    private String fullName;
    private String phone;
    private boolean active;
}
