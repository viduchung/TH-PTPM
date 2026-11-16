package com.football.bookingservice;

import com.football.bookingservice.support.FakePitchService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.football.bookingservice.support.FakePitchService.pitchKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH7 - booking gọi pitchs-service theo TÊN SERVICE "http://pitchs-service" qua Spring Cloud LoadBalancer.
 * Danh sách instance lấy từ SimpleDiscoveryClient (thay cho Eureka trong test) gồm 2 instance pitchs-service;
 * round-robin phải chia request cho cả hai.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Th7LoadBalancerIntegrationTest {

    private static final FakePitchService PITCH_NODE_1 = new FakePitchService("pitch-node-1");
    private static final FakePitchService PITCH_NODE_2 = new FakePitchService("pitch-node-2");
    private static final int REQUESTS = 6;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void discovery(DynamicPropertyRegistry registry) {
        registry.add("app.clients.pitch.base-url", () -> "http://pitchs-service");
        registry.add("app.clients.pitch.load-balanced", () -> "true");
        registry.add("spring.cloud.discovery.client.simple.instances.pitchs-service[0].uri", PITCH_NODE_1::baseUrl);
        registry.add("spring.cloud.discovery.client.simple.instances.pitchs-service[1].uri", PITCH_NODE_2::baseUrl);
    }

    @AfterAll
    static void stopNodes() {
        PITCH_NODE_1.close();
        PITCH_NODE_2.close();
    }

    @Test
    void requestsByServiceNameAreBalancedAcrossInstances() throws Exception {
        LocalDateTime start = LocalDateTime.of(LocalDate.now().plusDays(3), LocalTime.of(18, 0));
        for (int i = 0; i < REQUESTS; i++) {
            mvc.perform(get("/api/v1/bookings/quote").param("pitchId", "1")
                            .param("startTime", start.toString())
                            .param("endTime", start.plusHours(1).toString()))
                    .andExpect(status().isOk());
        }

        // Mỗi lần báo giá gọi pitchs-service 2 lần (GET sân + GET quote), round-robin xoay vòng trên từng lời gọi
        int node1 = PITCH_NODE_1.totalHits();
        int node2 = PITCH_NODE_2.totalHits();
        assertThat(node1 + node2).isEqualTo(2 * REQUESTS);
        assertThat(PITCH_NODE_1.hits(pitchKey(1)) + PITCH_NODE_2.hits(pitchKey(1))).isEqualTo(REQUESTS);
        assertThat(node1).as("instance 1 có nhận request").isPositive();
        assertThat(node2).as("instance 2 có nhận request").isPositive();
    }

    @Test
    void instanceInfoReportsServiceName() throws Exception {
        mvc.perform(get("/api/v1/bookings/instance-info")).andExpect(status().isOk());
    }
}
