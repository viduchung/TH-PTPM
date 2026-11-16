package com.football.bookingservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Cấu hình app.clients.* trong application.yml (địa chỉ service, timeout, retry). */
@Validated
@ConfigurationProperties(prefix = "app.clients")
public record ServiceClientsProperties(
        @Valid @NotNull Endpoint pitch,
        @Valid @NotNull Endpoint auth,
        @Min(1) int connectTimeoutMs,
        @Min(1) int responseTimeoutMs,
        @Min(0) int maxRetries,
        @Min(1) long retryBackoffMs) {

    /**
     * @param baseUrl      tên service trên Eureka (khi loadBalanced) hoặc địa chỉ thật
     * @param loadBalanced true: Spring Cloud LoadBalancer phân giải tên service thành instance (TH7)
     */
    public record Endpoint(@NotBlank String baseUrl, boolean loadBalanced) {
    }
}
