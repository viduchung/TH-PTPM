package com.pitchos.booking;

import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static com.pitchos.booking.support.FakePitchServer.ok;
import static com.pitchos.booking.support.FakePitchServer.pitchJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH5 - Chế độ chặt (booking.security.chusan-without-owner-info=false): không xác minh được chủ sân thì chặn 403. */
@TestPropertySource(properties = "booking.security.chusan-without-owner-info=false")
class Th5StrictOwnerPolicyIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Pitch chưa trả ownerId → chủ sân bị 403 khi duyệt và khi xem danh sách; ownerId khớp → 200")
    void strictMode_requiresVerifiedOwnership() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));

        review(id, OWNER).andExpect(status().isForbidden());
        getWithToken("/api/v1/bookings", TestTokens.owner(OWNER)).andExpect(status().isForbidden());

        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", OWNER)));
        review(id, OWNER).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions review(long id, long ownerId) throws Exception {
        return mvc.perform(patch("/api/v1/bookings/" + id + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.owner(ownerId)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"));
    }
}
