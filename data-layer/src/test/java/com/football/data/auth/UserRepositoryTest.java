package com.football.data.auth;

import com.football.data.auth.entity.Role;
import com.football.data.auth.entity.User;
import com.football.data.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Lưu và tìm kiếm người dùng theo username")
    void testSaveAndFindByUsername() {
        User user = User.builder()
                .username("hung_data_tester")
                .passwordHash("$2a$10$dummyHash...")
                .role(Role.ROLE_CUSTOMER)
                .fullName("Vi Đức Hùng")
                .phone("0988888888")
                .active(true)
                .build();

        User saved = userRepository.save(user);
        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findByUsername("hung_data_tester");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Vi Đức Hùng");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("Kiểm tra tồn tại theo username và số điện thoại")
    void testExistsByUsernameAndPhone() {
        User user = User.builder()
                .username("chusan_01")
                .passwordHash("hash")
                .role(Role.ROLE_FIELD_OWNER)
                .fullName("Lê Chủ Sân")
                .phone("0912345678")
                .active(true)
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByUsername("chusan_01")).isTrue();
        assertThat(userRepository.existsByUsername("non_existent")).isFalse();
        assertThat(userRepository.existsByPhone("0912345678")).isTrue();
        assertThat(userRepository.existsByPhone("0000000000")).isFalse();
    }

    @Test
    @DisplayName("Lọc danh sách người dùng theo vai trò (Role)")
    void testFindByRole() {
        userRepository.save(new User("userA", "hash", Role.ROLE_CUSTOMER, "Nguyen Van A", "0901111111"));
        userRepository.save(new User("userB", "hash", Role.ROLE_CUSTOMER, "Tran Van B", "0902222222"));
        userRepository.save(new User("adminA", "hash", Role.ROLE_ADMIN, "Admin Master", "0903333333"));

        List<User> customers = userRepository.findByRole(Role.ROLE_CUSTOMER);
        assertThat(customers).hasSize(2);

        List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
        assertThat(admins).hasSize(1);
    }
}
