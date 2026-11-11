-- ============================================================
--  auth_db - CSDL cho Auth Service (TH4)
--  Database-per-Service: auth-service toàn quyền quản lý auth_db
--  Phụ trách: Hoàng
--  Mật khẩu mẫu đã băm BCrypt tương ứng chuỗi gốc: password123
-- ============================================================

CREATE DATABASE IF NOT EXISTS `auth_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `auth_db`;

CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(50)   NOT NULL UNIQUE COMMENT 'Tên đăng nhập',
    `password_hash` VARCHAR(255)  NOT NULL COMMENT 'Mật khẩu băm BCrypt',
    `role`          VARCHAR(30)   NOT NULL COMMENT 'ROLE_CUSTOMER / ROLE_FIELD_OWNER / ROLE_ADMIN',
    `full_name`     VARCHAR(100)  NOT NULL COMMENT 'Họ và tên',
    `phone`         VARCHAR(15)   NOT NULL COMMENT 'Số điện thoại',
    `active`        BOOLEAN       NOT NULL DEFAULT TRUE,
    `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_username` (`username`),
    INDEX `idx_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tài khoản mẫu (mật khẩu: password123)
-- Lưu ý: ứng dụng tự seed khi khởi động nếu bảng trống (xem DataSeeder.java).
-- Dòng dưới chỉ để tham khảo khi nạp thủ công bằng hash có sẵn.
-- INSERT INTO users (username, password_hash, role, full_name, phone) VALUES
-- ('customer01', '$2a$10$...', 'ROLE_CUSTOMER',    'Nguyen Van An',  '0901234567'),
-- ('owner01',    '$2a$10$...', 'ROLE_FIELD_OWNER', 'Le Van Chu San', '0912345678'),
-- ('admin01',    '$2a$10$...', 'ROLE_ADMIN',       'Quan Tri Vien',  '0987654321');
