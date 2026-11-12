-- ============================================================
--  pitch_db - CSDL cho Pitchs Service (TH1 / TH4)
--  Database-per-Service: pitchs-service toàn quyền quản lý pitch_db
--  Phụ trách: Linh (Port: 8082)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `pitch_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pitch_db`;

CREATE TABLE IF NOT EXISTS `pitches` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)   NOT NULL COMMENT 'Tên sân bóng',
    `type`           VARCHAR(50)    NOT NULL COMMENT 'Loại sân: SAN_5, SAN_7, SAN_11',
    `price_per_hour` DECIMAL(12, 2) NOT NULL COMMENT 'Giá thuê mỗi giờ (VNĐ)',
    `address`        VARCHAR(255)   DEFAULT NULL COMMENT 'Địa chỉ / vị trí sân',
    `status`         VARCHAR(30)    NOT NULL DEFAULT 'AVAILABLE' COMMENT 'Trạng thái: AVAILABLE, BOOKED, MAINTENANCE',
    `description`    TEXT           DEFAULT NULL COMMENT 'Mô tả chi tiết đặc điểm sân',
    PRIMARY KEY (`id`),
    INDEX `idx_pitch_type` (`type`),
    INDEX `idx_pitch_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu mẫu (nếu nạp thủ công bằng SQL; Spring Boot cũng tự sinh qua DataSeeder nếu rỗng)
INSERT INTO `pitches` (`name`, `type`, `price_per_hour`, `address`, `status`, `description`) VALUES
('Sân 5 số 1 - Cỏ nhân tạo', 'SAN_5', 250000.00, 'Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội', 'AVAILABLE', 'Sân 5 người, mặt cỏ nhân tạo mới thay, có dàn đèn chiếu sáng ban đêm'),
('Sân 7 A - Tiêu chuẩn FIFA', 'SAN_7', 450000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'AVAILABLE', 'Sân 7 người chất lượng cao, thoát nước tốt khi trời mưa'),
('Sân 7 B - Cỏ nhân tạo', 'SAN_7', 400000.00, 'Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội', 'BOOKED', 'Sân 7 người, đang có lịch thi đấu giải phong trào'),
('Sân 11 Lớn - Mặt cỏ tự nhiên', 'SAN_11', 900000.00, 'Trung tâm huấn luyện thể thao Mỹ Đình, Nam Từ Liêm, Hà Nội', 'MAINTENANCE', 'Sân 11 người kích thước chuẩn quốc tế, đang bảo dưỡng mặt cỏ');

