package com.football.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ứng dụng Quản trị tầng Dữ liệu (Data Layer) - Hệ thống Quản lý đặt lịch sân bóng đá
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 * Kiến trúc: Database-per-Service / SOA
 */
@SpringBootApplication
public class FootballDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballDataApplication.class, args);
    }
}
