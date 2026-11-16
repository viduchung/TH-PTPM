package com.football.data.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TH7 Integration Test: Bộ lọc đóng dấu Instance (InstanceTrackingFilter)")
public class InstanceTrackingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TH7 Test 1: Mọi request HTTP đều được tự động đóng dấu Header X-Served-By-Instance và X-Service-Name")
    void testResponseHeaders_ContainInstanceMetadata() throws Exception {
        mockMvc.perform(get("/api/discovery/instance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Served-By-Instance"))
                .andExpect(header().exists("X-Service-Name"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.instanceId").exists())
                .andExpect(jsonPath("$.data.serviceName").exists());
    }

    @Test
    @DisplayName("TH7 Test 2: Endpoint Health trả về thông tin Discovery chuẩn hóa")
    void testHealthEndpoint_ReturnsDiscoveryInfo() throws Exception {
        mockMvc.perform(get("/api/discovery/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.heartbeatIntervalSeconds").value(10))
                .andExpect(jsonPath("$.data.evictionDurationSeconds").value(30));
    }
}

