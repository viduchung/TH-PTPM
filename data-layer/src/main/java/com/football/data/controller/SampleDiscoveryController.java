package com.football.data.controller;

import com.football.data.discovery.InstanceMetadataProvider;
import com.football.data.discovery.ServiceDiscoveryProperties;
import com.football.data.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller kiểm chứng phát hiện dịch vụ (Service Discovery) & Multi-Instance (TH7).
 * Cung cấp API truy xuất siêu dữ liệu của instance đang phục vụ request.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 7 (TH7)
 */
@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class SampleDiscoveryController {

    private final InstanceMetadataProvider instanceMetadataProvider;
    private final ServiceDiscoveryProperties discoveryProperties;

    /**
     * Endpoint lấy toàn bộ thông tin định danh instance đang phục vụ
     */
    @GetMapping("/instance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInstanceInfo() {
        Map<String, Object> metadata = instanceMetadataProvider.getMetadataMap();
        return ResponseEntity.ok(ApiResponse.success(metadata, "Truy xuất thông tin instance thành công"));
    }

    /**
     * Endpoint kiểm tra trạng thái sức khỏe và cấu hình Discovery
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDiscoveryHealth() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("serviceName", instanceMetadataProvider.getServiceName());
        healthInfo.put("instanceId", instanceMetadataProvider.getInstanceId());
        healthInfo.put("eurekaUrl", discoveryProperties.getEurekaServerUrl());
        healthInfo.put("heartbeatIntervalSeconds", discoveryProperties.getLeaseRenewalIntervalSeconds());
        healthInfo.put("evictionDurationSeconds", discoveryProperties.getLeaseExpirationDurationSeconds());

        return ResponseEntity.ok(ApiResponse.success(healthInfo, "Dịch vụ đang hoạt động bình thường"));
    }
}
