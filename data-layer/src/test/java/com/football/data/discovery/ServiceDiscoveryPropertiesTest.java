package com.football.data.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TH7 Unit Test: Chuẩn cấu hình Service Discovery (ServiceDiscoveryProperties)")
public class ServiceDiscoveryPropertiesTest {

    @Test
    @DisplayName("TH7 Test 1: Kiểm tra các giá trị cấu hình Eureka mặc định theo tiêu chuẩn hệ thống")
    void testDefaultDiscoveryProperties() {
        ServiceDiscoveryProperties properties = new ServiceDiscoveryProperties();

        assertEquals("http://localhost:8761/eureka/", properties.getEurekaServerUrl());
        assertTrue(properties.isPreferIpAddress());
        assertTrue(properties.isHealthCheckEnabled());

        // Kiểm tra cấu hình Fail-fast rút ngắn so với mặc định Spring Cloud (10s & 30s)
        assertEquals(10, properties.getLeaseRenewalIntervalSeconds(), "Heartbeat rút ngắn xuống 10s");
        assertEquals(30, properties.getLeaseExpirationDurationSeconds(), "Thời gian loại bỏ node chết 30s");
        assertEquals(10, properties.getRegistryFetchIntervalSeconds(), "Chu kỳ lấy danh sách 10s");
        assertNotNull(properties.getInstanceIdFormat());
    }

    @Test
    @DisplayName("TH7 Test 2: Tùy biến cấu hình cho môi trường Multi-Instance")
    void testCustomProperties() {
        ServiceDiscoveryProperties properties = new ServiceDiscoveryProperties();
        properties.setEurekaServerUrl("http://eureka-node1:8761/eureka/,http://eureka-node2:8761/eureka/");
        properties.setLeaseRenewalIntervalSeconds(5);
        properties.setLeaseExpirationDurationSeconds(15);

        assertTrue(properties.getEurekaServerUrl().contains("eureka-node1"));
        assertEquals(5, properties.getLeaseRenewalIntervalSeconds());
        assertEquals(15, properties.getLeaseExpirationDurationSeconds());
    }
}
