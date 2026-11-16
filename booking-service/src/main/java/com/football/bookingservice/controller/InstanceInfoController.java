package com.football.bookingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** TH7 - Cho biết instance booking-service nào đang phục vụ (kiểm tra chạy nhiều instance 8081 / 8083). */
@RestController
@Tag(name = "Instance", description = "Thông tin instance (TH7)")
public class InstanceInfoController {

    private final Environment environment;
    private final String configuredPort;

    public InstanceInfoController(Environment environment, @Value("${server.port:8081}") String configuredPort) {
        this.environment = environment;
        this.configuredPort = configuredPort;
    }

    @Operation(summary = "Port và tên service của instance đang phục vụ")
    @GetMapping("/api/v1/bookings/instance-info")
    public Map<String, Object> instanceInfo() {
        String activePort = environment.getProperty("local.server.port", configuredPort);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", environment.getProperty("spring.application.name", "booking-service"));
        info.put("activePort", activePort);
        info.put("status", "UP");
        return info;
    }
}
