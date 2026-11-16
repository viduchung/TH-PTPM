package com.football.bookingservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Đồng hồ theo múi giờ sân bóng; tách thành bean để test cố định được "bây giờ". */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
