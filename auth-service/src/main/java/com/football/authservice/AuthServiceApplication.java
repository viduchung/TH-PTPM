package com.football.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AUTH SERVICE - Dịch vụ Xác thực & Phân quyền
 * Hệ thống Quản lý đặt lịch sân bóng đá (Football Booking System)
 *
 * Phụ trách: Hoàng (Nhóm trưởng)  -  Port 8080
 * Nhánh Git: feat/hoang-auth-service
 *
 * Tổng hợp nhiệm vụ TH1-TH7:
 *  - TH1: CRUD tài khoản/người dùng, API đăng ký - đăng nhập cơ bản.
 *  - TH2: Xác thực liên service, truyền token giữa các service.
 *  - TH3: Validation đăng ký, chuẩn response lỗi 401/403.
 *  - TH4: auth_db - bảng users (user/role), quan hệ vai trò.
 *  - TH5: Phát hành & xác thực JWT, phân quyền RBAC (CUSTOMER/FIELD_OWNER/ADMIN).
 *  - TH6: Bảo mật liên service, token propagation (validate token, internal key).
 *  - TH7: Đăng ký service vào Eureka (service discovery).
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
