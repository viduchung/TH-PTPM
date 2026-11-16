package com.football.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger/OpenAPI 3 (TH3) với nút Authorize nhập Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API - Football Booking System")
                        .description("Dịch vụ Xác thực & Phân quyền (JWT, RBAC). Phụ trách: Hoàng.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .name(SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
