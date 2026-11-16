package com.football.pitchservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pitchs Service API - Football Booking System")
                        .version("1.0.0")
                        .description("Microservice Quản lý Sân bóng & Báo giá (Port 8082) - Môn SOA (TH1)")
                        .contact(new Contact()
                                .name("Linh - Pitchs Service")
                                .email("linh@footballbooking.vn")));
    }
}

