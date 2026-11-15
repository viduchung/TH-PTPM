package com.football.authservice.config;

import com.football.authservice.model.Role;
import com.football.authservice.model.User;
import com.football.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Nạp sẵn tài khoản mẫu khi DB trống (khớp dữ liệu demo của TH5).
 * Mật khẩu chung: password123
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("[DataSeeder] Đã có dữ liệu người dùng, bỏ qua khởi tạo mẫu.");
            return;
        }

        String pwd = passwordEncoder.encode("password123");
        userRepository.save(new User("customer01", pwd, Role.ROLE_CUSTOMER, "Nguyen Van An", "0901234567"));
        userRepository.save(new User("customer02", pwd, Role.ROLE_CUSTOMER, "Tran Thi Binh", "0901234568"));
        userRepository.save(new User("owner01", pwd, Role.ROLE_FIELD_OWNER, "Le Van Chu San", "0912345678"));
        userRepository.save(new User("admin01", pwd, Role.ROLE_ADMIN, "Quan Tri Vien", "0987654321"));

        log.info("[DataSeeder] Đã tạo 4 tài khoản mẫu (mật khẩu: password123): customer01, customer02, owner01, admin01");
    }
}
