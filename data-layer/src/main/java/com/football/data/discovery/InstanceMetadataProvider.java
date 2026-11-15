package com.football.data.discovery;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nhà cung cấp siêu dữ liệu định danh của Instance (Instance Metadata Provider).
 * 
 * Mỗi instance của microservice khi khởi động sẽ được cấp một Instance ID duy nhất,
 * gắn liền với cổng mạng (Port), IP và thời điểm khởi động.
 * 
 * Hỗ trợ các microservice:
 * 1. Đăng ký chính xác lên Eureka Server mà không bị trùng đè ID.
 * 2. Đóng dấu Header 'X-Served-By-Instance' vào HTTP Response để kiểm chứng Cân bằng tải (Load Balancing).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 7 (TH7)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class InstanceMetadataProvider {

    private final Environment environment;
    private final ServiceDiscoveryProperties discoveryProperties;

    private String instanceId;
    private String serviceName;
    private int port;
    private String hostAddress;
    private LocalDateTime startedAt;

    @PostConstruct
    public void init() {
        this.serviceName = environment.getProperty("spring.application.name", "football-service");
        String portStr = environment.getProperty("server.port", "8080");
        this.port = Integer.parseInt(portStr.equals("0") ? "8080" : portStr);

        try {
            this.hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            this.hostAddress = "127.0.0.1";
        }

        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        this.instanceId = String.format(discoveryProperties.getInstanceIdFormat(), serviceName, port, randomSuffix);
        this.startedAt = LocalDateTime.now();

        log.info("[Discovery] Khởi tạo Instance thành công: ID = [{}], Host = [{}:{}], Service = [{}]",
                instanceId, hostAddress, port, serviceName);
    }

    /**
     * Chuỗi đại diện instance phục vụ (Dùng cho Header HTTP X-Served-By-Instance)
     */
    public String getServingHeader() {
        return String.format("%s:%d [%s]", serviceName, port, instanceId);
    }

    /**
     * Xuất toàn bộ siêu dữ liệu của instance dưới dạng Map
     */
    public Map<String, Object> getMetadataMap() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("instanceId", instanceId);
        meta.put("serviceName", serviceName);
        meta.put("port", port);
        meta.put("hostAddress", hostAddress);
        meta.put("startedAt", startedAt.toString());
        meta.put("activeProfiles", environment.getActiveProfiles());
        meta.put("eurekaZone", discoveryProperties.getEurekaServerUrl());
        return meta;
    }
}
