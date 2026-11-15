package com.football.data.mapper;

import com.football.data.auth.entity.Role;
import com.football.data.auth.entity.User;
import com.football.data.dto.auth.UserResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserMapperTest {

    @Test
    @DisplayName("UserMapper: Chuyển đổi Entity sang DTO an toàn (Loại bỏ mật khẩu hash)")
    void testUserToDTO() {
        User user = User.builder()
                .id(10L)
                .username("hung_dev")
                .passwordHash("$2a$10$verySecretBCryptHashThatMustNeverLeak")
                .role(Role.ROLE_CUSTOMER)
                .fullName("Vũ Đức Hùng")
                .phone("0988888888")
                .active(true)
                .build();

        UserResponseDTO dto = UserMapper.toDTO(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUsername()).isEqualTo("hung_dev");
        assertThat(dto.getRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(dto.getRoleDisplayName()).isEqualTo("Khách hàng");
        assertThat(dto.getFullName()).isEqualTo("Vũ Đức Hùng");
        assertThat(dto.getPhone()).isEqualTo("0988888888");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("UserMapper: Xử lý an toàn khi đối tượng null")
    void testNullUser() {
        assertThat(UserMapper.toDTO(null)).isNull();
    }
}
