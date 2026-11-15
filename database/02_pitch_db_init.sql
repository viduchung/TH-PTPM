-- ============================================================
-- CƠ SỞ DỮ LIỆU: pitch_db (Phụ trách: Pitchs Service - Port 8082)
-- Kiến trúc: Microservices / Database-per-Service
-- Thành viên thiết kế tầng Data: Hùng (feat/hung-data)
-- Hệ QT CSDL: MySQL 8.0+ / MariaDB / UTF8MB4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `pitch_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pitch_db`;

-- ------------------------------------------------------------
-- Bảng 1: pitches (Quản lý danh sách sân bóng đá)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `golden_hour_prices`;
DROP TABLE IF EXISTS `pitches`;

CREATE TABLE `pitches` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính định danh sân bóng',
    `name`           VARCHAR(100)   NOT NULL COMMENT 'Tên sân bóng (ví dụ: Sân 5 số 1)',
    `type`           VARCHAR(50)    NOT NULL COMMENT 'Loại sân: SAN_5, SAN_7, SAN_11',
    `price_per_hour` DECIMAL(12, 2) NOT NULL COMMENT 'Giá thuê cơ bản mỗi giờ (VNĐ)',
    `address`        VARCHAR(255)   DEFAULT NULL COMMENT 'Địa chỉ, vị trí cụm sân',
    `status`         VARCHAR(30)    NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Trạng thái: AVAILABLE, BOOKED, MAINTENANCE',
    `description`    TEXT           DEFAULT NULL COMMENT 'Mô tả chi tiết tiện ích sân',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm thêm sân',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật gần nhất',
    PRIMARY KEY (`id`),
    INDEX `idx_pitches_type` (`type`),
    INDEX `idx_pitches_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng danh sách sân bóng';

-- ------------------------------------------------------------
-- Bảng 2: golden_hour_prices (Bảng giá giờ vàng áp dụng cho từng sân)
-- Quan hệ 1-N nội bộ service: 1 Pitch có nhiều GoldenHourPrice
-- ------------------------------------------------------------
CREATE TABLE `golden_hour_prices` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính bảng giá giờ vàng',
    `pitch_id`       BIGINT         NOT NULL COMMENT 'Khóa ngoại nội bộ trỏ tới pitches(id)',
    `day_of_week`    VARCHAR(50)    NOT NULL COMMENT 'Ngày áp dụng: ALL_DAYS, WEEKDAY, WEEKEND',
    `start_time`     TIME           NOT NULL COMMENT 'Giờ bắt đầu khung giờ vàng',
    `end_time`       TIME           NOT NULL COMMENT 'Giờ kết thúc khung giờ vàng',
    `price_per_hour` DECIMAL(12, 2) NOT NULL COMMENT 'Đơn giá thuê trong khung giờ vàng (VNĐ)',
    `description`    VARCHAR(255)   DEFAULT NULL COMMENT 'Ghi chú khung giờ',
    PRIMARY KEY (`id`),
    INDEX `idx_ghp_pitch_id` (`pitch_id`),
    CONSTRAINT `fk_ghp_pitches`
        FOREIGN KEY (`pitch_id`) REFERENCES `pitches` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng cấu hình khung giá giờ vàng nội bộ pitch_db';

-- ------------------------------------------------------------
-- Dữ liệu khởi tạo ban đầu (Seed Data mẫu)
-- ------------------------------------------------------------
INSERT INTO `pitches` (`id`, `name`, `type`, `price_per_hour`, `address`, `status`, `description`) VALUES
(1, 'Sân 5 số 1 - Cỏ nhân tạo', 'SAN_5', 250000.00, 'Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội', 'AVAILABLE', 'Sân 5 người, mặt cỏ đạt chuẩn, dàn đèn LED chiếu sáng ban đêm'),
(2, 'Sân 7 A - Tiêu chuẩn FIFA mini', 'SAN_7', 450000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'AVAILABLE', 'Sân 7 người chất lượng cao, có khán đài và hệ thống thoát nước tốt'),
(3, 'Sân 7 B - Cỏ nhân tạo', 'SAN_7', 400000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'BOOKED', 'Sân 7 người, hiện đang có lịch thi đấu giao hữu'),
(4, 'Sân 11 Lớn - Cỏ tự nhiên', 'SAN_11', 900000.00, 'Trung tâm Thể thao Mỹ Đình, Nam Từ Liêm, Hà Nội', 'MAINTENANCE', 'Sân 11 người kích thước chuẩn quốc tế, đang trong chu kỳ bảo dưỡng mặt cỏ');

INSERT INTO `golden_hour_prices` (`pitch_id`, `day_of_week`, `start_time`, `end_time`, `price_per_hour`, `description`) VALUES
(1, 'ALL_DAYS', '17:30:00', '20:30:00', 320000.00, 'Khung giờ cao điểm buổi tối các ngày trong tuần'),
(2, 'WEEKDAY',  '17:00:00', '21:00:00', 550000.00, 'Khung giờ vàng từ thứ 2 đến thứ 6'),
(2, 'WEEKEND',  '06:00:00', '10:00:00', 500000.00, 'Khung giờ thể thao sáng sớm thứ Bảy & Chủ Nhật');
