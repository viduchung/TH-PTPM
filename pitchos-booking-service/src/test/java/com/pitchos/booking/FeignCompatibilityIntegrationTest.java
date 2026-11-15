package com.pitchos.booking;

import com.pitchos.booking.client.FeignPitchGateway;
import com.pitchos.booking.client.PitchGateway;
import com.pitchos.booking.support.FakePitchServer;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tương thích: cơ chế OpenFeign cũ (pitch.client.type=feign) vẫn chạy đúng như bản PTPM trước. */
@TestPropertySource(properties = "pitch.client.type=feign")
class FeignCompatibilityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PitchGateway pitchGateway;

    @Test
    @DisplayName("OpenFeign: đặt sân 201, sân không tồn tại 404 (CustomErrorDecoder), Pitch lỗi 500 → 503")
    void feignClientStillWorks() throws Exception {
        assertThat(pitchGateway).isInstanceOf(FeignPitchGateway.class);

        postBooking(TestTokens.user(USER_A), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(300000.00));
        postBooking(TestTokens.user(USER_A), bookingJson(99, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isNotFound());

        PITCH.setDefault(2, FakePitchServer.status(500));
        postBooking(TestTokens.user(USER_A), bookingJson(2, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isServiceUnavailable());
    }
}
