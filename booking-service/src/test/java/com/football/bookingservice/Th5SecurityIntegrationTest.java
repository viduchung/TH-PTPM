package com.football.bookingservice;

import com.football.bookingservice.support.FakeAuthService;
import com.football.bookingservice.support.IntegrationTestBase;
import com.football.bookingservice.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH5 - Bảo vệ endpoint booking theo vai trò: CUSTOMER đặt sân, OWNER duyệt đơn, ADMIN toàn quyền. */
class Th5SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void missingOrInvalidTokenReturns401InStandardFormat() throws Exception {
        mvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"));
        getWithToken("/api/v1/bookings", TestTokens.signedWithWrongKey(CUSTOMER_A))
                .andExpect(status().isUnauthorized());
        getWithToken("/api/v1/bookings", TestTokens.expiredCustomer(CUSTOMER_A))
                .andExpect(status().isUnauthorized());
        getWithToken("/api/v1/bookings", "not-a-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        mvc.perform(get("/api/v1/bookings/instance-info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("booking-service"));
        mvc.perform(get("/api/v1/bookings/pitches/1/schedule").param("date", at(0, 0, 0).toLocalDate().toString()))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void ownerAndAdminCannotCreateBookings() throws Exception {
        postBooking(TestTokens.owner(OWNER), bookingJson(1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
        postBooking(TestTokens.admin(ADMIN), bookingJson(1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isForbidden());
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void unknownRoleIsNeverTreatedAsPrivileged() throws Exception {
        String guest = TestTokens.withRole(9, "ROLE_GUEST");
        getWithToken("/api/v1/bookings", guest).andExpect(status().isForbidden());
        postBooking(guest, bookingJson(1, at(0, 8, 0), at(0, 9, 0))).andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadOrCancelSomeoneElsesBooking() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        String other = TestTokens.bearer(TestTokens.customer(CUSTOMER_B));

        mvc.perform(get("/api/v1/bookings/" + id).header("Authorization", other))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel").header("Authorization", other))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyOwnerOrAdminCanReviewBookings() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        review(id, TestTokens.customer(CUSTOMER_A), "DA_DUYET").andExpect(status().isForbidden());
        review(id, TestTokens.owner(OWNER), "DA_DUYET")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DA_DUYET"));
        review(id, TestTokens.admin(ADMIN), "TU_CHOI").andExpect(status().isConflict());
    }

    @Test
    void reviewCannotBeUsedToCancel() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        review(id, TestTokens.owner(OWNER), "DA_HUY").andExpect(status().isBadRequest());
    }

    @Test
    void ownerCannotCancelButAdminCan() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel")
                        .header("Authorization", TestTokens.bearer(TestTokens.owner(OWNER))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel")
                        .header("Authorization", TestTokens.bearer(TestTokens.admin(ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void ownerSeesAllBookingsAndInternalServiceCanReadWithServiceKey() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(CUSTOMER_B, 2, at(0, 8, 0), at(0, 9, 0));

        getWithToken("/api/v1/bookings", TestTokens.owner(OWNER))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(get("/api/v1/bookings/" + id).header("X-Internal-Service-Key", TestTokens.INTERNAL_KEY))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/bookings/" + id).header("X-Internal-Service-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    /** Token thật của auth-service không có userId: booking hỏi /api/v1/auth/me (token propagation) và nhớ tạm. */
    @Test
    void authServiceTokenWithoutUserIdIsResolvedThroughAuthMe() throws Exception {
        String token = TestTokens.authStyle("customer_th5_me", "ROLE_CUSTOMER");
        AUTH.register(token, 42, "customer_th5_me", "ROLE_CUSTOMER", true);

        postBooking(token, bookingJson(1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(42));
        getWithToken("/api/v1/bookings/my", token)
                .andExpect(jsonPath("$.data.totalElements").value(1));

        assertThat(AUTH.hits(FakeAuthService.ME_KEY)).as("lần 2 dùng cache, không gọi lại auth").isEqualTo(1);
        assertThat(AUTH.lastHeader("Authorization")).isEqualTo("Bearer " + token);
    }

    @Test
    void authServiceTokenOfLockedAccountIsRejected() throws Exception {
        String token = TestTokens.authStyle("customer_th5_locked", "ROLE_CUSTOMER");
        AUTH.register(token, 43, "customer_th5_locked", "ROLE_CUSTOMER", false);

        postBooking(token, bookingJson(1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa"));
    }

    @Test
    void authServiceDownMeansNoBookingInsteadOfGuessingUser() throws Exception {
        String token = TestTokens.authStyle("customer_th5_unknown", "ROLE_CUSTOMER");

        postBooking(token, bookingJson(1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isUnauthorized());
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void ownerTokenFromAuthServiceUsesFieldOwnerRole() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        String ownerToken = TestTokens.authStyle("owner01", "ROLE_FIELD_OWNER");

        review(id, ownerToken, "DA_DUYET").andExpect(status().isOk());
        assertThat(AUTH.hits(FakeAuthService.ME_KEY)).as("duyệt đơn không cần users.id").isZero();
    }

    private org.springframework.test.web.servlet.ResultActions review(long id, String token, String target)
            throws Exception {
        return mvc.perform(patch("/api/v1/bookings/" + id + "/status")
                .header("Authorization", TestTokens.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + target + "\"}"));
    }
}
