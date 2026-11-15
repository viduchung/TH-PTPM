-- ============================================================
-- CƠ SỞ DỮ LIỆU: auth_db (Phụ trách: Auth Service - Port 8080)
-- Kiến trúc: Microservices / Database-per-Service
-- Thành viên thiết kế tầng Data: Hùng (feat/hung-data)
-- Hệ QT CSDL: MySQL 8.0+ / MariaDB / UTF8MB4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `auth_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `auth_db`;

-- ------------------------------------------------------------
-- Bảng: users (Quản lý người dùng, phân quyền RBAC)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính định danh người dùng',
    `username`      VARCHAR(50)   NOT NULL COMMENT 'Tên đăng nhập (duy nhất)',
    `password_hash` VARCHAR(255)  NOT NULL COMMENT 'Mật khẩu đã băm bằng BCrypt',
    `role`          VARCHAR(30)   NOT NULL COMMENT 'ROLE_CUSTOMER, ROLE_FIELD_OWNER, ROLE_ADMIN, ROLE_INTERNAL_SERVICE',
    `full_name`     VARCHAR(100)  NOT NULL COMMENT 'Họ và tên đầy đủ',
    `phone`         VARCHAR(15)   NOT NULL COMMENT 'Số điện thoại liên hệ',
    `active`        BOOLEAN       NOT NULL DEFAULT TRUE COMMENT 'Trạng thái hoạt động (1: Hoạt động, 0: Khóa)',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo tài khoản',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật gần nhất',
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_username` UNIQUE (`username`),
    INDEX `idx_users_username` (`username`),
    INDEX `idx_users_phone` (`phone`),
    INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng lưu thông tin tài khoản người dùng';

-- ------------------------------------------------------------
-- Dữ liệu khởi tạo ban đầu (Seed Data mẫu - Password: password123)
-- Hash BCrypt: $2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe
-- ------------------------------------------------------------
INSERT INTO `users` (`id`, `username`, `password_hash`, `role`, `full_name`, `phone`, `active`) VALUES
(1, 'customer01', '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_CUSTOMER', 'Nguyễn Văn An (Khách đặt sân)', '0901234567', 1),
(2, 'customer02', '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_CUSTOMER', 'Trần Thị Bình (Khách hàng)', '0909876543', 1),
(3, 'owner01',    '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_FIELD_OWNER', 'Lê Văn Chủ Sân (Quản lý cụm sân)', '0912345678', 1),
(4, 'admin01',    '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_ADMIN', 'Phạm Hoàng Admin (Quản trị hệ thống)', '0987654321', 1);

