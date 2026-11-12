package com.football.pitchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // TH7: Bật tính năng Service Discovery để đăng ký vào Eureka Server
public class PitchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PitchServiceApplication.class, args);
    }
}
