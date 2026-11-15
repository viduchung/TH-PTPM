package com.football.data.mapper;

import com.football.data.auth.entity.User;
import com.football.data.dto.auth.UserResponseDTO;

/**
 * Mapper an toàn chuyển đổi giữa Entity User và DTO UserResponseDTO
 * 
 * NGUYÊN TẮC BẢO MẬT & ĐÓNG GÓI:
 * - Tuyệt đối không map passwordHash ra ngoài DTO.
 * - Xử lý kiểm tra null-safe toàn diện.
 */
public final class UserMapper {

    private UserMapper() {
        // Utility class
    }

    public static UserResponseDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .roleDisplayName(user.getRole() != null ? user.getRole().getDisplayName() : null)
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .active(user.isActive())
                .build();
    }
}
