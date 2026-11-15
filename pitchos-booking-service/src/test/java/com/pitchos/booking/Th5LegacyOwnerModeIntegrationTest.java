package com.pitchos.booking;

import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.support.FakePitchServer;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static com.pitchos.booking.support.FakePitchServer.ok;
import static com.pitchos.booking.support.FakePitchServer.pitchJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH5 - CHẾ ĐỘ TƯƠNG THÍCH KHÔNG AN TOÀN (booking.security.chusan-without-owner-info=true), chỉ dùng khi demo với
 * Pitch Service cũ chưa có ownerId. Các test ở đây trước giai đoạn khắc phục 2 chạy ở cấu hình mặc định (khi đó = true);
 * nay chuyển sang lớp này để vẫn ghi nhận đúng hành vi (và rủi ro) của chế độ tương thích, không bị xóa.
 */
@TestPropertySource(properties = "booking.security.chusan-without-owner-info=true")
class Th5LegacyOwnerModeIntegrationTest extends IntegrationTestBase {

    private static final String BOOKINGS = "/api/v1/bookings";

    /** Trước đây: Th5SecurityIntegrationTest.ownerWithoutOwnerInfo_allowedByDefault. */
    @Test
    @DisplayName("Chế độ tương thích: Pitch CHƯA có ownerId → chủ sân vẫn duyệt được và xem được danh sách (như bản cũ)")
    void legacyMode_ownerWithoutOwnerInfo_allowed() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        review(id, TestTokens.owner(OWNER)).andExpect(status().isOk());
        getWithToken(BOOKINGS, TestTokens.owner(OWNER)).andExpect(status().isOk());
    }

    /** Trước đây: Th5JwtClaimsAndRoleIntegrationTest.defaultMode_anyOwnerCanReviewAndViewWhenPitchHasNoOwnerId. */
    @Test
    @DisplayName("RỦI RO của chế độ tương thích: chủ sân BẤT KỲ duyệt được và xem được đơn khi Pitch chưa có ownerId")
    void legacyMode_anyOwnerCanReviewAndViewWhenPitchHasNoOwnerId() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        String unrelatedOwner = TestTokens.owner(99L);

        getWithToken(BOOKINGS + "/" + id, unrelatedOwner).andExpect(status().isOk());
        getWithToken(BOOKINGS, unrelatedOwner).andExpect(jsonPath("$.totalElements").value(1));
        review(id, unrelatedOwner).andExpect(status().isOk());
    }

    /** Trước đây: Th5JwtClaimsAndRoleIntegrationTest.defaultMode_ownerListIsNotFilteredByOwnedPitches. */
    @Test
    @DisplayName("RỦI RO của chế độ tương thích: danh sách của chủ sân gồm cả đơn của sân khác")
    void legacyMode_ownerListIsNotFilteredByOwnedPitches() throws Exception {
        createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_A, 2, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", OWNER)));
        PITCH.setDefault(2, ok(pitchJson(2, "500000.00", "hoat_dong", 77L)));

        getWithToken(BOOKINGS, TestTokens.owner(OWNER)).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Kể cả ở chế độ tương thích: Pitch lỗi khi kiểm tra chủ sân → 503, KHÔNG cấp quyền, đơn không đổi")
    void legacyMode_pitchFailureStillDeniesOwner() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, FakePitchServer.status(500));

        review(id, TestTokens.owner(OWNER)).andExpect(status().isServiceUnavailable());
        assertThat(bookingRepository.findById(id)).get()
                .extracting(b -> b.getStatus()).isEqualTo(BookingStatus.cho_duyet);
    }

    private ResultActions review(long id, String token) throws Exception {
        return mvc.perform(patch(BOOKINGS + "/" + id + "/status")
                .header("Authorization", TestTokens.bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"));
    }
}
