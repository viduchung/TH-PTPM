package com.football.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * EUREKA SERVER - Service Registry cho hệ thống sân bóng đá (TH7).
 * Phụ trách: Hoàng (Nhóm trưởng)  -  Port 8761
 *
 * Các service (auth 8080, booking 8081, pitchs 8082...) đăng ký vào đây,
 * sau đó gọi lẫn nhau qua service-name thay vì hard-code host:port.
 *
 * Dashboard:  http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
