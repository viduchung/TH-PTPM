package com.pitchos.booking;

import com.pitchos.booking.config.TimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PitchosBookingServiceApplication {
    public static void main(String[] args) {
        TimeConfig.applyBusinessTimeZoneAsDefault();
        SpringApplication.run(PitchosBookingServiceApplication.class, args);
    }
}
