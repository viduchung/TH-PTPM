package com.football.data.discovery;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình chuẩn hóa Service Discovery (Eureka Client) dùng chung cho toàn bộ hệ thống.
 * Áp dụng thống nhất cho auth-service, pitchs-service, booking-service, payment-service.
 * 
 * Giúp các microservice tự động đăng ký và tìm kiếm nhau một cách ổn định,
 * phát hiện nhanh sự cố (Fail-fast eviction) khi instance bị chết hoặc khởi động lại.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 7 (TH7)
 */
@Configuration
@ConfigurationProperties(prefix = "football.discovery")
@Getter
@Setter
public class ServiceDiscoveryProperties {

    /** Địa chỉ URL của Eureka Discovery Server */
    private String eurekaServerUrl = "http://localhost:8761/eureka/";

    /** Ưu tiên sử dụng địa chỉ IP thay vì Hostname trong môi trường Container/Docker */
    private boolean preferIpAddress = true;

    /**
     * Nhịp tim gửi tín hiệu sống (Heartbeat - giây).
     * Mặc định hạ xuống 10 giây (so với 30s của Spring Cloud) để phát hiện instance chết nhanh chóng.
     */
    private int leaseRenewalIntervalSeconds = 10;

    /**
     * Thời gian tối đa Eureka Server chờ nhịp tim trước khi gạch tên instance chết (giây).
     * Mặc định hạ xuống 30 giây (so với 90s mặc định) để tránh định tuyến nhầm vào node chết.
     */
    private int leaseExpirationDurationSeconds = 30;

    /**
     * Chu kỳ đồng bộ lại danh sách dịch vụ (Registry Cache) từ Eureka Server (giây).
     */
    private int registryFetchIntervalSeconds = 10;

    /** Kích hoạt kiểm tra sức khỏe thông qua Actuator Health Indicator */
    private boolean healthCheckEnabled = true;

    /** Mẫu định danh duy nhất của instance: ${service-name}:${port}:${uuid} */
    private String instanceIdFormat = "%s:%d:%s";
}

