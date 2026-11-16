-- ============================================================
--  pitch_db - CSDL cho Pitchs Service (TH1, TH2, TH3, TH4)
--  Database-per-Service: pitchs-service toàn quyền quản lý pitch_db
--  Phụ trách: Linh (Port: 8082)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `pitch_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pitch_db`;

-- ------------------------------------------------------------
--  Bảng 1: Sân bóng (pitches)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pitches` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)   NOT NULL COMMENT 'Tên sân bóng',
    `type`           VARCHAR(50)    NOT NULL COMMENT 'Loại sân: SAN_5, SAN_7, SAN_11',
    `price_per_hour` DECIMAL(12, 2) NOT NULL COMMENT 'Giá thuê cơ bản mỗi giờ (VNĐ)',
    `address`        VARCHAR(255)   DEFAULT NULL COMMENT 'Địa chỉ / vị trí sân',
    `status`         VARCHAR(30)    NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Trạng thái: AVAILABLE, BOOKED, MAINTENANCE',
    `description`    TEXT           DEFAULT NULL COMMENT 'Mô tả chi tiết đặc điểm sân',
    PRIMARY KEY (`id`),
    INDEX `idx_pitch_type` (`type`),
    INDEX `idx_pitch_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
--  Bảng 2: Bảng giá giờ vàng (golden_hour_prices) - TH4
--  Quan hệ 1-N: 1 Sân bóng có nhiều Khung giá giờ vàng
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `golden_hour_prices` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `pitch_id`       BIGINT         NOT NULL COMMENT 'Mã sân bóng (FK)',
    `day_of_week`    VARCHAR(50)    NOT NULL COMMENT 'Ngày áp dụng: ALL_DAYS, WEEKDAY, WEEKEND',
    `start_time`     TIME           NOT NULL COMMENT 'Giờ bắt đầu khung giờ vàng',
    `end_time`       TIME           NOT NULL COMMENT 'Giờ kết thúc khung giờ vàng',
    `price_per_hour` DECIMAL(12, 2) NOT NULL COMMENT 'Giá áp dụng trong giờ vàng (VNĐ)',
    `description`    VARCHAR(255)   DEFAULT NULL COMMENT 'Mô tả khung giờ',
    PRIMARY KEY (`id`),
    INDEX `idx_ghp_pitch_id` (`pitch_id`),
    CONSTRAINT `fk_ghp_pitch`
        FOREIGN KEY (`pitch_id`) REFERENCES `pitches` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
--  Dữ liệu mẫu (nếu nạp thủ công bằng SQL script)
-- ------------------------------------------------------------
INSERT INTO `pitches` (`id`, `name`, `type`, `price_per_hour`, `address`, `status`, `description`) VALUES
(1, 'Sân 5 số 1 - Cỏ nhân tạo', 'SAN_5', 250000.00, 'Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội', 'AVAILABLE', 'Sân 5 người, mặt cỏ nhân tạo mới thay, có dàn đèn chiếu sáng ban đêm'),
(2, 'Sân 7 A - Tiêu chuẩn FIFA', 'SAN_7', 450000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'AVAILABLE', 'Sân 7 người chất lượng cao, thoát nước tốt khi trời mưa'),
(3, 'Sân 7 B - Cỏ nhân tạo', 'SAN_7', 400000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'BOOKED', 'Sân 7 người, đang có lịch thi đấu giải phong trào'),
(4, 'Sân 11 Lớn - Mặt cỏ tự nhiên', 'SAN_11', 900000.00, 'Trung tâm huấn luyện thể thao Mỹ Đình, Nam Từ Liêm, Hà Nội', 'MAINTENANCE', 'Sân 11 người kích thước chuẩn quốc tế, đang bảo dưỡng mặt cỏ');

INSERT INTO `golden_hour_prices` (`pitch_id`, `day_of_week`, `start_time`, `end_time`, `price_per_hour`, `description`) VALUES
(1, 'ALL_DAYS', '17:30:00', '20:30:00', 320000.00, 'Khung giờ cao điểm buổi tối sau giờ hành chính'),
(2, 'WEEKDAY',  '17:00:00', '21:00:00', 550000.00, 'Khung giờ vàng các ngày trong tuần (T2 - T6)'),
(2, 'WEEKEND',  '06:00:00', '10:00:00', 500000.00, 'Khung giờ sáng sớm cuối tuần (Thứ 7, CN)');
