package com.pitchos.booking;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.pitchos.booking.support.FakePitchServer.ok;
import static com.pitchos.booking.support.FakePitchServer.pitchJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TH5 - Bổ sung: token thiếu thông tin, token hết hạn / sai chữ ký trên MỌI endpoint bảo vệ, role lạ,
 * và hành vi của chính sách chủ sân ở cấu hình MẶC ĐỊNH (fail-closed, chusan-without-owner-info=false).
 */
class Th5JwtClaimsAndRoleIntegrationTest extends IntegrationTestBase {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String INVALID_TOKEN = "Token không hợp lệ";

    @Test
    @DisplayName("Token ký đúng nhưng THIẾU userId và sub là username (đúng như Auth PTPM phát) → 401, không tạo đơn")
    void tokenWithoutUserId_andUsernameSubject_returns401() throws Exception {
        String token = TestTokens.withClaims("user", null, "user", TestTokens.validForOneHour());

        getWithToken(BOOKINGS + "/my-bookings", token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(INVALID_TOKEN));
        postBooking(token, bookingJson(1, at(0, 8, 0), at(0, 9, 0))).andExpect(status().isUnauthorized());

        assertThat(PITCH.hits()).isZero();
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Token THIẾU userId nhưng sub là số → được chấp nhận (cơ chế dự phòng có sẵn của JwtUtil, userId = sub)")
    void tokenWithoutUserId_butNumericSubject_usesSubjectAsUserId() throws Exception {
        String token = TestTokens.withClaims("3", null, "user", TestTokens.validForOneHour());

        postBooking(token, bookingJson(1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(3));
    }

    @Test
    @DisplayName("Token THIẾU role hoặc role rỗng → 401 trên endpoint bảo vệ, không tạo đơn")
    void tokenWithoutRole_returns401() throws Exception {
        String noRole = TestTokens.withClaims("user", USER_A, null, TestTokens.validForOneHour());
        String blankRole = TestTokens.withClaims("user", USER_A, "", TestTokens.validForOneHour());

        for (String token : new String[]{noRole, blankRole}) {
            getWithToken(BOOKINGS + "/my-bookings", token)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(INVALID_TOKEN));
            postBooking(token, bookingJson(1, at(0, 8, 0), at(0, 9, 0))).andExpect(status().isUnauthorized());
        }
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Token HẾT HẠN và token SAI CHỮ KÝ → 401 trên tất cả endpoint bảo vệ, dữ liệu không đổi, không gọi Pitch")
    void expiredAndForgedTokens_cannotReachAnyProtectedEndpoint() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        int pitchHitsBefore = PITCH.hits();

        for (String token : new String[]{TestTokens.expiredUser(USER_A), TestTokens.signedWithWrongKey(USER_A)}) {
            for (MockHttpServletRequestBuilder request : protectedRequests(id)) {
                mvc.perform(request.header("Authorization", TestTokens.bearer(token)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.status").value(401));
            }
        }

        assertThat(PITCH.hits()).isEqualTo(pitchHitsBefore);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(bookingRepository.findById(id)).get()
                .extracting(Booking::getStatus).hasToString("cho_duyet");
    }

    @Test
    @DisplayName("Role LẠ (không thuộc user/chusan/admin, kể cả viết hoa) KHÔNG được xem danh sách đơn → 403")
    void unknownRole_cannotListBookings() throws Exception {
        createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_B, 2, at(0, 8, 0), at(0, 9, 0));

        for (String role : new String[]{"guest", "CUSTOMER", "ADMIN"}) {
            String token = TestTokens.withClaims("x", 50L, role, TestTokens.validForOneHour());
            getWithToken(BOOKINGS, token)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
            postBooking(token, bookingJson(1, at(1, 8, 0), at(1, 9, 0))).andExpect(status().isForbidden());
        }
    }

    /**
     * Cùng kịch bản với "defaultMode_anyOwnerCanReviewAndView…" trước giai đoạn khắc phục 2 (khi mặc định = true).
     * Hành vi cũ chuyển sang Th5LegacyOwnerModeIntegrationTest; ở mặc định mới (fail-closed) rủi ro đã bị chặn.
     */
    @Test
    @DisplayName("Mặc định fail-closed, Pitch chưa có ownerId: chủ sân KHÔNG liên quan không xem / liệt kê / duyệt được → 403")
    void defaultMode_unrelatedOwnerIsDeniedWhenPitchHasNoOwnerId() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        String unrelatedOwner = TestTokens.owner(99L);

        getWithToken(BOOKINGS + "/" + id, unrelatedOwner).andExpect(status().isForbidden());
        getWithToken(BOOKINGS, unrelatedOwner).andExpect(status().isForbidden());
        review(id, unrelatedOwner).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mặc định fail-closed: dù Pitch trả ownerId, danh sách GET /bookings của chủ sân bị chặn (chưa lọc được theo sân sở hữu)")
    void defaultMode_ownerListIsDeniedBecauseItCannotBeFilteredByOwnedPitches() throws Exception {
        createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_A, 2, at(0, 8, 0), at(0, 9, 0));
        PITCH.setDefault(1, ok(pitchJson(1, "300000.00", "hoat_dong", OWNER)));
        PITCH.setDefault(2, ok(pitchJson(2, "500000.00", "hoat_dong", 77L)));

        getWithToken(BOOKINGS, TestTokens.owner(OWNER)).andExpect(status().isForbidden());
    }

    private ResultActions review(long id, String token) throws Exception {
        return mvc.perform(patch(BOOKINGS + "/" + id + "/status")
                .header("Authorization", TestTokens.bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"));
    }

    private MockHttpServletRequestBuilder[] protectedRequests(long id) {
        String body = bookingJson(1, at(1, 8, 0), at(1, 9, 0));
        return new MockHttpServletRequestBuilder[]{
                post(BOOKINGS).contentType(MediaType.APPLICATION_JSON).content(body),
                get(BOOKINGS + "/my-bookings"),
                get(BOOKINGS),
                get(BOOKINGS + "/" + id),
                get(BOOKINGS + "/quote?pitchId=1&startTime=" + at(1, 8, 0) + "&endTime=" + at(1, 9, 0)),
                put(BOOKINGS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body),
                patch(BOOKINGS + "/" + id + "/cancel"),
                delete(BOOKINGS + "/" + id),
                patch(BOOKINGS + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"da_duyet\"}")
        };
    }
}
