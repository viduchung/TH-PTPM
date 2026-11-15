package com.pitchos.booking;

import com.pitchos.booking.support.FakePitchServer;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH7 - Gọi Pitch Service theo TÊN DỊCH VỤ qua Spring Cloud LoadBalancer với 2 instance thật (2 HTTP server).
 * Danh sách instance lấy từ SimpleDiscoveryClient (cùng giao diện DiscoveryClient mà Eureka cung cấp);
 * phần đăng ký / tra cứu bằng Eureka Server thật được kiểm thử riêng (xem báo cáo hoàn thành).
 */
class Th7LoadBalancerIntegrationTest extends IntegrationTestBase {

    private static final FakePitchServer INSTANCE_A = FakePitchServer.start("pitch-A");
    private static final FakePitchServer INSTANCE_B = FakePitchServer.start("pitch-B");
    private static final int CALLS = 6;

    @DynamicPropertySource
    static void discoveryProperties(DynamicPropertyRegistry registry) {
        registry.add("pitch.service.discovery-enabled", () -> "true");
        registry.add("pitch.service.service-id", () -> "pitchos-pitch-service");
        registry.add("spring.cloud.discovery.client.simple.instances.pitchos-pitch-service[0].uri", INSTANCE_A::baseUrl);
        registry.add("spring.cloud.discovery.client.simple.instances.pitchos-pitch-service[1].uri", INSTANCE_B::baseUrl);
    }

    @AfterAll
    static void stopInstances() {
        INSTANCE_A.close();
        INSTANCE_B.close();
    }

    @Test
    @DisplayName("Round-robin chia đều 6 lời gọi cho 2 instance; tắt 1 instance thì retry chuyển sang instance còn sống")
    void requestsAreBalancedAndFailOver() throws Exception {
        String quoteUrl = "/api/v1/bookings/quote?pitchId=1&startTime=" + at(0, 18, 0) + "&endTime=" + at(0, 19, 0);

        for (int i = 0; i < CALLS; i++) {
            getWithToken(quoteUrl, TestTokens.user(USER_A)).andExpect(status().isOk());
        }
        assertThat(INSTANCE_A.hits()).isEqualTo(CALLS / 2);
        assertThat(INSTANCE_B.hits()).isEqualTo(CALLS / 2);
        assertThat(PITCH.hits()).as("không gọi URL cố định khi bật discovery").isZero();

        INSTANCE_B.close();
        int hitsOnABefore = INSTANCE_A.hits();
        for (int i = 0; i < CALLS; i++) {
            getWithToken(quoteUrl, TestTokens.user(USER_A)).andExpect(status().isOk());
        }
        assertThat(INSTANCE_A.hits() - hitsOnABefore).isEqualTo(CALLS);

        // Đặt sân qua service-name cũng hoạt động
        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0))).andExpect(status().isCreated());
    }
}
