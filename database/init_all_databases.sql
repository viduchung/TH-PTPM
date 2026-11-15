-- ============================================================
-- TỔNG HỢP KHỞI TẠO CƠ SỞ DỮ LIỆU TOÀN BỘ HỆ THỐNG
-- Đề tài: Quản lý đặt lịch sân bóng đá (Football Booking System)
-- Kiến trúc: Microservices / Database-per-Service
-- Thành viên phụ trách: HÙNG (Data Administrator & Backend Data Engineer)
-- Nhánh Git: feat/hung-data
-- File này tổng hợp khởi tạo 3 cơ sở dữ liệu vật lý độc lập:
--   1. auth_db     (Phụ trách: Hoàng - auth-service @ 8080)
--   2. pitch_db    (Phụ trách: Linh  - pitchs-service @ 8082)
--   3. booking_db  (Phụ trách: Nhật  - booking-service @ 8081/8083)
-- ============================================================

-- ------------------------------------------------------------
-- PHẦN 1: CSDL auth_db
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `auth_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `auth_db`;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(50)   NOT NULL,
    `password_hash` VARCHAR(255)  NOT NULL,
    `role`          VARCHAR(30)   NOT NULL,
    `full_name`     VARCHAR(100)  NOT NULL,
    `phone`         VARCHAR(15)   NOT NULL,
    `active`        BOOLEAN       NOT NULL DEFAULT TRUE,
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_users_username` UNIQUE (`username`),
    INDEX `idx_users_username` (`username`),
    INDEX `idx_users_phone` (`phone`),
    INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `users` (`id`, `username`, `password_hash`, `role`, `full_name`, `phone`, `active`) VALUES
(1, 'customer01', '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_CUSTOMER', 'Nguyễn Văn An (Khách đặt sân)', '0901234567', 1),
(2, 'customer02', '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_CUSTOMER', 'Trần Thị Bình (Khách hàng)', '0909876543', 1),
(3, 'owner01',    '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_FIELD_OWNER', 'Lê Văn Chủ Sân (Quản lý cụm sân)', '0912345678', 1),
(4, 'admin01',    '$2a$10$wOqZ3w8/Z2n8wY6mD6LGe.ZgN2/i0iQ.F3FjPZ6v.n6b3q1Z.F.Oe', 'ROLE_ADMIN', 'Phạm Hoàng Admin (Quản trị hệ thống)', '0987654321', 1);

-- ------------------------------------------------------------
-- PHẦN 2: CSDL pitch_db
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `pitch_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pitch_db`;

DROP TABLE IF EXISTS `golden_hour_prices`;
DROP TABLE IF EXISTS `pitches`;

CREATE TABLE `pitches` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)   NOT NULL,
    `type`           VARCHAR(50)    NOT NULL,
    `price_per_hour` DECIMAL(12, 2) NOT NULL,
    `address`        VARCHAR(255)   DEFAULT NULL,
    `status`         VARCHAR(30)    NOT NULL DEFAULT 'AVAILABLE',
    `description`    TEXT           DEFAULT NULL,
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_pitches_type` (`type`),
    INDEX `idx_pitches_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `golden_hour_prices` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `pitch_id`       BIGINT         NOT NULL,
    `day_of_week`    VARCHAR(50)    NOT NULL,
    `start_time`     TIME           NOT NULL,
    `end_time`       TIME           NOT NULL,
    `price_per_hour` DECIMAL(12, 2) NOT NULL,
    `description`    VARCHAR(255)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_ghp_pitch_id` (`pitch_id`),
    CONSTRAINT `fk_ghp_pitches`
        FOREIGN KEY (`pitch_id`) REFERENCES `pitches` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `pitches` (`id`, `name`, `type`, `price_per_hour`, `address`, `status`, `description`) VALUES
(1, 'Sân 5 số 1 - Cỏ nhân tạo', 'SAN_5', 250000.00, 'Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội', 'AVAILABLE', 'Sân 5 người, mặt cỏ đạt chuẩn, dàn đèn LED chiếu sáng ban đêm'),
(2, 'Sân 7 A - Tiêu chuẩn FIFA mini', 'SAN_7', 450000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'AVAILABLE', 'Sân 7 người chất lượng cao, có khán đài và hệ thống thoát nước tốt'),
(3, 'Sân 7 B - Cỏ nhân tạo', 'SAN_7', 400000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'BOOKED', 'Sân 7 người, hiện đang có lịch thi đấu giao hữu'),
(4, 'Sân 11 Lớn - Cỏ tự nhiên', 'SAN_11', 900000.00, 'Trung tâm Thể thao Mỹ Đình, Nam Từ Liêm, Hà Nội', 'MAINTENANCE', 'Sân 11 người kích thước chuẩn quốc tế, đang trong chu kỳ bảo dưỡng mặt cỏ');

INSERT INTO `golden_hour_prices` (`pitch_id`, `day_of_week`, `start_time`, `end_time`, `price_per_hour`, `description`) VALUES
(1, 'ALL_DAYS', '17:30:00', '20:30:00', 320000.00, 'Khung giờ cao điểm buổi tối các ngày trong tuần'),
(2, 'WEEKDAY',  '17:00:00', '21:00:00', 550000.00, 'Khung giờ vàng từ thứ 2 đến thứ 6'),
(2, 'WEEKEND',  '06:00:00', '10:00:00', 500000.00, 'Khung giờ thể thao sáng sớm thứ Bảy & Chủ Nhật');

-- ------------------------------------------------------------
-- PHẦN 3: CSDL booking_db
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `booking_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `booking_db`;

DROP TABLE IF EXISTS `bookings`;
CREATE TABLE `bookings` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT         NOT NULL,
    `pitch_id`       BIGINT         NOT NULL,
    `start_time`     DATETIME       NOT NULL,
    `end_time`       DATETIME       NOT NULL,
    `total_price`    DECIMAL(12, 2) NOT NULL,
    `price_per_hour` DECIMAL(12, 2) DEFAULT NULL,
    `status`         VARCHAR(30)    NOT NULL DEFAULT 'CHO_DUYET',
    `note`           VARCHAR(255)   DEFAULT NULL,
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_bookings_pitch_time` (`pitch_id`, `start_time`, `end_time`),
    INDEX `idx_bookings_user_id` (`user_id`),
    INDEX `idx_bookings_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `bookings` (`id`, `user_id`, `pitch_id`, `start_time`, `end_time`, `total_price`, `price_per_hour`, `status`, `note`) VALUES
(1, 1, 1, '2026-11-20 18:00:00', '2026-11-20 19:30:00', 375000.00, 250000.00, 'DA_DUYET', 'Đội bóng sinh viên K65 Bách Khoa giao hữu'),
(2, 2, 2, '2026-11-21 19:00:00', '2026-11-21 21:00:00', 900000.00, 450000.00, 'CHO_DUYET', 'Thuê sân 7 đá giải văn phòng công ty'),
(3, 1, 1, '2026-11-22 07:00:00', '2026-11-22 08:30:00', 375000.00, 250000.00, 'TU_CHOI', 'Trùng lịch nội bộ sân, đã từ chối');

