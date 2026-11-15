-- ============================================================
-- CƠ SỞ DỮ LIỆU: payment_db (Phụ trách: Payment Service - Port 8084 / Mở rộng TH4)
-- Kiến trúc: Microservices / Database-per-Service
-- Thành viên thiết kế tầng Data: HÙNG (feat/hung-data) - Tuần 4 (TH4)
-- Hệ QT CSDL: MySQL 8.0+ / MariaDB / UTF8MB4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `payment_db`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `payment_db`;

-- ------------------------------------------------------------
-- Bảng: payments (Quản lý giao dịch thanh toán tiền sân)
-- 
-- NGUYÊN TẮC DATABASE-PER-SERVICE:
-- 1. Lưu `booking_id` (BIGINT) dạng Logical ID trỏ sang booking_db.
-- 2. TUYỆT ĐỐI KHÔNG TẠO FOREIGN KEY VẬT LÝ sang booking_db.bookings(id).
-- 3. Mọi thao tác xác thực đơn đặt sân phải thông qua REST API của Booking Service.
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `payments`;

CREATE TABLE `payments` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'Mã giao dịch thanh toán nội bộ (Khóa chính)',
    `booking_id`       BIGINT         NOT NULL COMMENT 'Logical ID đơn đặt sân (trỏ sang booking_db.bookings.id)',
    `amount`           DECIMAL(12, 2) NOT NULL COMMENT 'Số tiền thanh toán thực tế (VNĐ)',
    `payment_method`   VARCHAR(30)    NOT NULL COMMENT 'VNPAY, MOMO, CASH, BANKING',
    `status`           VARCHAR(30)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SUCCESS, FAILED',
    `transaction_code` VARCHAR(100)   NOT NULL COMMENT 'Mã đối soát giao dịch ngân hàng / cổng thanh toán (Duy nhất)',
    `payment_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm thực hiện giao dịch',
    `note`             VARCHAR(255)   DEFAULT NULL COMMENT 'Ghi chú giao dịch',
    `created_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo bản ghi',
    `updated_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_payments_transaction_code` UNIQUE (`transaction_code`),
    INDEX `idx_payments_booking_id` (`booking_id`),
    INDEX `idx_payments_status` (`status`),
    INDEX `idx_payments_time` (`payment_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng quản lý giao dịch thanh toán';

-- ------------------------------------------------------------
-- Dữ liệu khởi tạo ban đầu (Seed Data mẫu)
-- ------------------------------------------------------------
INSERT INTO `payments` (`id`, `booking_id`, `amount`, `payment_method`, `status`, `transaction_code`, `payment_time`, `note`) VALUES
(1, 1, 375000.00, 'VNPAY',   'SUCCESS', 'TXN_VNPAY_20261120_001', '2026-11-20 17:45:00', 'Thanh toan tien coc san 5 don #1'),
(2, 2, 900000.00, 'BANKING', 'SUCCESS', 'TXN_MB_20261121_002',    '2026-11-21 18:30:00', 'Chuyen khoan toan bo tien thue san 7 don #2'),
(3, 3, 375000.00, 'MOMO',    'FAILED',  'TXN_MOMO_20261122_003',  '2026-11-22 06:45:00', 'Giao dich that bai do het thoi gian cho');
