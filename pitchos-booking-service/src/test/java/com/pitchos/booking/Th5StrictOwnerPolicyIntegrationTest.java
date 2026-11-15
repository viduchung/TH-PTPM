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

    @Test
    @DisplayName("Fail-closed: chủ sân không xác minh được (Pitch chưa có ownerId) không xem được chi tiết đơn → 403")
    void strictMode_ownerCannotViewWithoutVerifiedOwnership() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));

        getWithToken("/api/v1/bookings/" + id, TestTokens.owner(OWNER)).andExpect(status().isForbidden());
        getWithToken("/api/v1/bookings/" + id, TestTokens.owner(99L)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fail-closed: Pitch Service lỗi khi kiểm tra chủ sân → 503, đơn KHÔNG bị đổi trạng thái")
    void strictMode_pitchFailureDoesNotGrantOwnerAccess() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, com.pitchos.booking.support.FakePitchServer.status(500));

        review(id, OWNER).andExpect(status().isServiceUnavailable());
        org.assertj.core.api.Assertions.assertThat(bookingRepository.findById(id)).get()
                .extracting(b -> b.getStatus().name()).isEqualTo("cho_duyet");
    }

    @Test
    @DisplayName("Chế độ chặt không ảnh hưởng admin: admin vẫn duyệt được, không cần gọi Pitch")
    void strictMode_adminStillReviews() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        int hitsBefore = PITCH.hits();

        mvc.perform(patch("/api/v1/bookings/" + id + "/status")
                        .header("Authorization", TestTokens.bearer(TestTokens.admin(ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(PITCH.hits()).isEqualTo(hitsBefore);
    }

    private org.springframework.test.web.servlet.ResultActions review(long id, long ownerId) throws Exception {
        return mvc.perform(patch("/api/v1/bookings/" + id + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.owner(ownerId)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"));
    }
}
