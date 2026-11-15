-- ============================================================
-- CƠ SỞ DỮ LIỆU: booking_db (Phụ trách: Booking Service - Port 8081 / 8083)
-- Kiến trúc: Microservices / Database-per-Service
-- Thành viên thiết kế tầng Data: Hùng (feat/hung-data)
-- Hệ QT CSDL: MySQL 8.0+ / MariaDB / UTF8MB4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `booking_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `booking_db`;

-- ------------------------------------------------------------
-- Bảng: bookings (Quản lý các lượt đặt sân bóng)
-- 
-- NGUYÊN TẮC DATABASE-PER-SERVICE CỦA HÙNG:
-- 1. Lưu `user_id` (BIGINT) dạng Logical ID trỏ sang auth_db.
-- 2. Lưu `pitch_id` (BIGINT) dạng Logical ID trỏ sang pitch_db.
-- 3. TUYỆT ĐỐI KHÔNG TẠO FOREIGN KEY VẬT LÝ xuyên database để đảm bảo
--    tính bao đóng (Encapsulation) và khả năng độc lập triển khai.
-- 4. Đánh chỉ mục Index tổ hợp (pitch_id, start_time, end_time) để
--    tối ưu hóa hiệu năng câu truy vấn chống trùng lịch thi đấu.
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `bookings`;

CREATE TABLE `bookings` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Mã đơn đặt sân (Khóa chính)',
    `user_id`        BIGINT         NOT NULL COMMENT 'Logical ID người đặt (tương ứng users.id bên auth_db)',
    `pitch_id`       BIGINT         NOT NULL COMMENT 'Logical ID sân bóng (tương ứng pitches.id bên pitch_db)',
    `start_time`     DATETIME       NOT NULL COMMENT 'Thời gian bắt đầu nhận sân',
    `end_time`       DATETIME       NOT NULL COMMENT 'Thời gian kết thúc trả sân',
    `total_price`    DECIMAL(12, 2) NOT NULL COMMENT 'Tổng số tiền thuê sân (VNĐ)',
    `price_per_hour` DECIMAL(12, 2) DEFAULT NULL COMMENT 'Đơn giá thuê sân tại thời điểm chốt đơn',
    `status`         VARCHAR(30)    NOT NULL DEFAULT 'CHO_DUYET' COMMENT 'CHO_DUYET, DA_DUYET, TU_CHOI, DA_HUY',
    `note`           VARCHAR(255)   DEFAULT NULL COMMENT 'Ghi chú thêm từ khách hàng',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo yêu cầu đặt sân',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật trạng thái',
    PRIMARY KEY (`id`),
    -- Index tối ưu thuật toán kiểm tra trùng lịch (TH1 & TH4 của Hùng)
    INDEX `idx_bookings_pitch_time` (`pitch_id`, `start_time`, `end_time`),
    -- Index truy vấn lịch sử đơn đặt theo khách hàng
    INDEX `idx_bookings_user_id` (`user_id`),
    -- Index lọc theo trạng thái đơn
    INDEX `idx_bookings_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng quản lý đơn đặt sân bóng đá';

-- ------------------------------------------------------------
-- Dữ liệu khởi tạo ban đầu (Seed Data mẫu)
-- ------------------------------------------------------------
INSERT INTO `bookings` (`id`, `user_id`, `pitch_id`, `start_time`, `end_time`, `total_price`, `price_per_hour`, `status`, `note`) VALUES
(1, 1, 1, '2026-11-20 18:00:00', '2026-11-20 19:30:00', 375000.00, 250000.00, 'DA_DUYET', 'Đội bóng sinh viên K65 Bách Khoa giao hữu'),
(2, 2, 2, '2026-11-21 19:00:00', '2026-11-21 21:00:00', 900000.00, 450000.00, 'CHO_DUYET', 'Thuê sân 7 đá giải văn phòng công ty'),
(3, 1, 1, '2026-11-22 07:00:00', '2026-11-22 08:30:00', 375000.00, 250000.00, 'TU_CHOI', 'Trùng lịch nội bộ sân, đã từ chối');
