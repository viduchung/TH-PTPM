package com.pitchos.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Múi giờ nghiệp vụ của hệ thống đặt sân: Asia/Ho_Chi_Minh, cố định, KHÔNG phụ thuộc múi giờ mặc định
 * của máy hoặc Docker container (container thường là UTC → nếu dùng múi giờ máy sẽ lệch 7 giờ).
 */
@Configuration
public class TimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Đồng hồ nghiệp vụ (giờ Việt Nam) dùng cho quy tắc "giờ bắt đầu phải ở tương lai". */
    @Bean
    public Clock clock() {
        return businessClock(Clock.systemUTC());
    }

    /** Cùng một thời điểm của base, nhưng đọc theo giờ Việt Nam. */
    public static Clock businessClock(Clock base) {
        return base.withZone(BUSINESS_ZONE);
    }

    /**
     * Đặt múi giờ mặc định của JVM = giờ Việt Nam khi khởi động (gọi trong main), để các thời điểm do thư viện tự tạo
     * bằng múi giờ mặc định (created_at / updated_at của Hibernate, timestamp trong ErrorResponse) cũng theo giờ Việt Nam,
     * khớp với serverTimezone=Asia/Ho_Chi_Minh trong chuỗi kết nối MySQL.
     */
    public static void applyBusinessTimeZoneAsDefault() {
        TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_ZONE));
    }
}
