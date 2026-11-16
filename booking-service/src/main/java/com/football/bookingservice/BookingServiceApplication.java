package com.football.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Booking Service (Port 8081) - Nhật phụ trách.
 *  - TH1: CRUD đơn đặt sân, xem lịch đặt theo sân / ngày.
 *  - TH2: gọi pitchs-service bằng WebClient kiểm tra sân và lấy giá.
 *  - TH3: phân trang, lọc, validation ngày giờ.
 *  - TH4: booking_db, Logical ID (user_id, pitch_id), không Foreign Key xuyên DB.
 *  - TH5: JWT dùng chung với auth-service, phân quyền CUSTOMER / OWNER / ADMIN.
 *  - TH6: Consumer của /quote, xử lý lỗi liên service, Idempotency-Key chống double-booking.
 *  - TH7: đăng ký Eureka, gọi pitchs-service theo service-name qua Spring Cloud LoadBalancer.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
