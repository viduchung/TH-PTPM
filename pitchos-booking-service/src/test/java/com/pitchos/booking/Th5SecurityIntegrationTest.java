package com.pitchos.booking;

import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.pitchos.booking.support.FakePitchServer.ok;
import static com.pitchos.booking.support.FakePitchServer.pitchJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH5 - JWT (token của Auth Service) và phân quyền user / chusan / admin, quyền sở hữu Booking. */
class Th5SecurityIntegrationTest extends IntegrationTestBase {

    private static final String BOOKINGS = "/api/v1/bookings";

    @Test
    @DisplayName("Không có token → 401 JSON; token sai chữ ký → 401; token hết hạn → 401 'hết hạn'")
    void missingInvalidExpiredToken_return401Json() throws Exception {
        mvc.perform(get(BOOKINGS + "/my-bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Chưa đăng nhập hoặc thiếu token (Authorization: Bearer <token>)"));
        mvc.perform(get(BOOKINGS + "/my-bookings").header("Authorization", "Bearer khong.phai.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token không hợp lệ"));
        mvc.perform(get(BOOKINGS + "/my-bookings")
                        .header("Authorization", TestTokens.bearer(TestTokens.signedWithWrongKey(USER_A))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token không hợp lệ"));
        mvc.perform(get(BOOKINGS + "/my-bookings")
                        .header("Authorization", TestTokens.bearer(TestTokens.expiredUser(USER_A))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token đã hết hạn, vui lòng đăng nhập lại"));
    }

    @Test
    @DisplayName("Không có token thì không gọi Pitch Service và không tạo đơn")
    void unauthenticatedCreate_doesNothing() throws Exception {
        mvc.perform(post(BOOKINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(1, at(0, 8, 0), at(0, 9, 0))))
                .andExpect(status().isUnauthorized());
        assertThat(PITCH.hits()).isZero();
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Sai vai trò → 403 JSON: user duyệt đơn, chusan/admin đặt sân, chusan sửa đơn")
    void wrongRole_returns403Json() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));

        mvc.perform(patch(BOOKINGS + "/" + id + "/status")
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
        postBooking(TestTokens.owner(OWNER), bookingJson(1, at(1, 8, 0), at(1, 9, 0))).andExpect(status().isForbidden());
        postBooking(TestTokens.admin(ADMIN), bookingJson(1, at(1, 8, 0), at(1, 9, 0))).andExpect(status().isForbidden());
        mvc.perform(put(BOOKINGS + "/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.owner(OWNER)))
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(1, at(1, 8, 0), at(1, 9, 0))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User xem / sửa / hủy đơn của người khác → 403; danh sách của user luôn chỉ có đơn của mình")
    void userCannotTouchOthersBookings() throws Exception {
        long bookingOfA = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_B, 2, at(0, 8, 0), at(0, 9, 0));
        String tokenB = TestTokens.user(USER_B);

        getWithToken(BOOKINGS + "/" + bookingOfA, tokenB).andExpect(status().isForbidden());
        mvc.perform(put(BOOKINGS + "/" + bookingOfA).header("Authorization", TestTokens.bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(1, at(1, 8, 0), at(1, 9, 0))))
                .andExpect(status().isForbidden());
        mvc.perform(patch(BOOKINGS + "/" + bookingOfA + "/cancel").header("Authorization", TestTokens.bearer(tokenB)))
                .andExpect(status().isForbidden());

        // Client cố truyền userId của người khác: bị bỏ qua, vẫn chỉ thấy đơn của chính mình
        getWithToken(BOOKINGS + "?userId=" + USER_A, tokenB)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(USER_B));
    }

    @Test
    @DisplayName("userId trong body bị bỏ qua: đơn luôn thuộc userId trong JWT")
    void userIdFromBodyIsIgnored() throws Exception {
        String bodyWithForeignUser = "{\"userId\":999,\"pitchId\":1,\"startTime\":\"" + at(0, 8, 0)
                + "\",\"endTime\":\"" + at(0, 9, 0) + "\"}";
        postBooking(TestTokens.user(USER_A), bodyWithForeignUser)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(USER_A));
    }

    @Test
    @DisplayName("Chủ sân: Pitch trả ownerId khác → 403 khi duyệt / xem; ownerId đúng → 200")
    void ownerCanOnlyReviewOwnPitch_whenPitchProvidesOwnerId() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", 77L)));

        reviewAsOwner(id, OWNER).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không phải chủ của sân trong đơn đặt này"));
        getWithToken(BOOKINGS + "/" + id, TestTokens.owner(OWNER)).andExpect(status().isForbidden());

        reviewAsOwner(id, 77L).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("da_duyet"));
    }

    /**
     * Trước giai đoạn khắc phục 2, test này là "ownerWithoutOwnerInfo_allowedByDefault" (mặc định cũ = true).
     * Hành vi cũ vẫn được kiểm thử trong Th5LegacyOwnerModeIntegrationTest (cấu hình tương thích bật rõ ràng).
     */
    @Test
    @DisplayName("Chủ sân khi Pitch CHƯA có ownerId: cấu hình MẶC ĐỊNH (fail-closed) từ chối duyệt và từ chối danh sách → 403")
    void ownerWithoutOwnerInfo_deniedByDefault() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        reviewAsOwner(id, OWNER).andExpect(status().isForbidden());
        getWithToken(BOOKINGS, TestTokens.owner(OWNER)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin: xem mọi đơn, lọc theo userId, duyệt không cần gọi Pitch, hủy đơn của khách")
    void adminHasAdministrativeAccess() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_B, 2, at(0, 8, 0), at(0, 9, 0));
        String admin = TestTokens.admin(ADMIN);

        getWithToken(BOOKINGS, admin).andExpect(jsonPath("$.totalElements").value(2));
        getWithToken(BOOKINGS + "?userId=" + USER_B, admin).andExpect(jsonPath("$.totalElements").value(1));
        getWithToken(BOOKINGS + "/" + id, admin).andExpect(status().isOk());

        int hitsBefore = PITCH.hits();
        mvc.perform(patch(BOOKINGS + "/" + id + "/status").header("Authorization", TestTokens.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"))
                .andExpect(status().isOk());
        assertThat(PITCH.hits()).isEqualTo(hitsBefore);
        mvc.perform(patch(BOOKINGS + "/" + id + "/cancel").header("Authorization", TestTokens.bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("da_huy"));
    }

    private org.springframework.test.web.servlet.ResultActions reviewAsOwner(long id, long ownerId) throws Exception {
        return mvc.perform(patch(BOOKINGS + "/" + id + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.owner(ownerId)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"));
    }
}
