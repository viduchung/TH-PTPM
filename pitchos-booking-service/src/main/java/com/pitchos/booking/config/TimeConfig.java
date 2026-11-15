package com.pitchos.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Đồng hồ hệ thống; tách thành bean để kiểm thử quy tắc thời gian không phụ thuộc giờ máy. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
