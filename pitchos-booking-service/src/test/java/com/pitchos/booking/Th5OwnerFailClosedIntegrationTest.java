package com.pitchos.booking;

import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.support.FakePitchServer;
import com.pitchos.booking.support.FakePitchServer.Reply;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.pitchos.booking.support.FakePitchServer.delayed;
import static com.pitchos.booking.support.FakePitchServer.ok;
import static com.pitchos.booking.support.FakePitchServer.pitchJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH5 - Chính sách chủ sân FAIL-CLOSED ở cấu hình MẶC ĐỊNH (không đặt booking.security.chusan-without-owner-info):
 * chủ sân chỉ được thao tác khi Pitch Service trả ownerId trùng userId trong JWT; mọi trường hợp khác đều bị từ chối.
 */
class Th5OwnerFailClosedIntegrationTest extends IntegrationTestBase {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final long OTHER_OWNER = 77L;

    @Test
    @DisplayName("(1) Pitch KHÔNG trả ownerId → chủ sân không xem chi tiết, không duyệt, không từ chối được (403), đơn không đổi")
    void noOwnerId_ownerCannotViewOrReview() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));

        getWithToken(BOOKINGS + "/" + id, TestTokens.owner(OWNER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Chưa xác minh được chủ sân (Pitch Service chưa cung cấp ownerId) nên không cho phép thao tác"));
        review(id, OWNER, "da_duyet").andExpect(status().isForbidden());
        review(id, OWNER, "tu_choi").andExpect(status().isForbidden());

        assertStatus(id, BookingStatus.cho_duyet);
    }

    @Test
    @DisplayName("(2)(6) Pitch lỗi 500/503/404, JSON hỏng, dữ liệu sai, timeout, thiếu ownerId → KHÔNG trường hợp nào cấp quyền")
    void pitchFailures_neverGrantOwnerAccess() throws Exception {
        String validOwnedPitch = pitchJson(1, "300000.00", "hoat_dong", OWNER);
        Map<String, Reply> failures = new LinkedHashMap<>();
        failures.put("500", FakePitchServer.status(500));
        failures.put("503", FakePitchServer.status(503));
        failures.put("404", FakePitchServer.status(404));
        failures.put("JSON hỏng", ok("{khong-phai-json"));
        failures.put("thiếu giá", ok(pitchJson(1, null, "hoat_dong", OWNER)));
        failures.put("thiếu ownerId", ok(pitchJson(1, "300000.00", "hoat_dong", null)));
        failures.put("timeout", delayed(validOwnedPitch, 3_000));

        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        for (Map.Entry<String, Reply> failure : failures.entrySet()) {
            PITCH.setDefault(1, failure.getValue());

            int viewStatus = getWithToken(BOOKINGS + "/" + id, TestTokens.owner(OWNER))
                    .andReturn().getResponse().getStatus();
            int reviewStatus = review(id, OWNER, "da_duyet").andReturn().getResponse().getStatus();

            assertThat(viewStatus).as("xem chi tiết khi Pitch: " + failure.getKey()).isIn(403, 404, 503);
            assertThat(reviewStatus).as("duyệt khi Pitch: " + failure.getKey()).isIn(403, 404, 503);
            assertStatus(id, BookingStatus.cho_duyet);
        }
    }

    @Test
    @DisplayName("(3) Pitch trả ownerId KHÁC → chủ sân bị từ chối xem, duyệt, từ chối (403)")
    void differentOwnerId_ownerIsDenied() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", OTHER_OWNER)));

        getWithToken(BOOKINGS + "/" + id, TestTokens.owner(OWNER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không phải chủ của sân trong đơn đặt này"));
        review(id, OWNER, "da_duyet").andExpect(status().isForbidden());
        review(id, OWNER, "tu_choi").andExpect(status().isForbidden());
        assertStatus(id, BookingStatus.cho_duyet);
    }

    @Test
    @DisplayName("(4) Pitch trả ownerId TRÙNG → chủ sân xem chi tiết, duyệt và từ chối được (200)")
    void matchingOwnerId_ownerIsAllowed() throws Exception {
        long approvedId = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        long rejectedId = createBooking(USER_A, 1, at(0, 10, 0), at(0, 11, 0));
        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", OWNER)));

        getWithToken(BOOKINGS + "/" + approvedId, TestTokens.owner(OWNER)).andExpect(status().isOk());
        review(approvedId, OWNER, "da_duyet").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("da_duyet"));
        review(rejectedId, OWNER, "tu_choi").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("tu_choi"));
    }

    @Test
    @DisplayName("(5) Admin vẫn xem, liệt kê, duyệt, hủy khi Pitch không trả ownerId, kể cả khi Pitch đang lỗi")
    void admin_worksWithoutOwnerIdAndWhilePitchIsDown() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        long other = createBooking(USER_B, 1, at(0, 10, 0), at(0, 11, 0));
        PITCH.setDefault(1, FakePitchServer.status(500));
        String admin = TestTokens.admin(ADMIN);

        getWithToken(BOOKINGS + "/" + id, admin).andExpect(status().isOk());
        getWithToken(BOOKINGS, admin).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(patch(BOOKINGS + "/" + id + "/status").header("Authorization", TestTokens.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(BOOKINGS + "/" + other + "/cancel").header("Authorization", TestTokens.bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("(7) Danh sách GET /bookings ở mặc định: chủ sân 403 (kể cả khi Pitch có ownerId); user chỉ thấy đơn mình; admin thấy tất cả")
    void listEndpoint_behaviourInFailClosedMode() throws Exception {
        createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_B, 2, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", OWNER)));

        getWithToken(BOOKINGS, TestTokens.owner(OWNER)).andExpect(status().isForbidden());
        getWithToken(BOOKINGS + "?pitchId=1", TestTokens.owner(OWNER)).andExpect(status().isForbidden());
        getWithToken(BOOKINGS, TestTokens.user(USER_A))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(USER_A));
        getWithToken(BOOKINGS, TestTokens.admin(ADMIN)).andExpect(jsonPath("$.totalElements").value(2));
    }

    private ResultActions review(long id, long ownerId, String newStatus) throws Exception {
        return mvc.perform(patch(BOOKINGS + "/" + id + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.owner(ownerId)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + newStatus + "\"}"));
    }

    private void assertStatus(long id, BookingStatus expected) {
        assertThat(bookingRepository.findById(id)).get().extracting(b -> b.getStatus()).isEqualTo(expected);
    }
}
