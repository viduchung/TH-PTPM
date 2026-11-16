package com.football.bookingservice;

import com.football.bookingservice.support.IntegrationTestBase;
import com.football.bookingservice.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH3 - Phân trang, lọc, sắp xếp danh sách booking; validation ngày giờ và dữ liệu đầu vào. */
class Th3PagingValidationIntegrationTest extends IntegrationTestBase {

    private String adminToken;

    @BeforeEach
    void seed() throws Exception {
        adminToken = TestTokens.admin(ADMIN);
        createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(CUSTOMER_A, 2, at(0, 10, 0), at(0, 11, 0));
        createBooking(CUSTOMER_B, 1, at(1, 8, 0), at(1, 9, 0));
        long rejected = createBooking(CUSTOMER_B, 2, at(2, 8, 0), at(2, 9, 0));
        mvc.perform(patch("/api/v1/bookings/" + rejected + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.owner(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"TU_CHOI\"}")).andExpect(status().isOk());
    }

    @Test
    void paginatesWithDefaultSortByStartTime() throws Exception {
        mvc.perform(get("/api/v1/bookings").param("page", "0").param("size", "3")
                        .header("Authorization", TestTokens.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.content[0].startTime").value(at(0, 8, 0).toString() + ":00"));
    }

    @Test
    void filtersByStatusPitchAndDate() throws Exception {
        mvc.perform(get("/api/v1/bookings").param("status", "TU_CHOI")
                        .header("Authorization", TestTokens.bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mvc.perform(get("/api/v1/bookings").param("pitchId", "1")
                        .header("Authorization", TestTokens.bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mvc.perform(get("/api/v1/bookings").param("date", at(0, 0, 0).toLocalDate().toString())
                        .header("Authorization", TestTokens.bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mvc.perform(get("/api/v1/bookings").param("from", at(1, 0, 0).toString()).param("to", at(3, 0, 0).toString())
                        .header("Authorization", TestTokens.bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void sortsDescendingByTotalPrice() throws Exception {
        mvc.perform(get("/api/v1/bookings").param("sort", "totalPrice,desc")
                        .header("Authorization", TestTokens.bearer(adminToken)))
                .andExpect(jsonPath("$.data.content[0].totalPrice").value(500000.00));
    }

    @Test
    void customerListIsScopedToOwnBookingsEvenIfAnotherUserIdIsRequested() throws Exception {
        mvc.perform(get("/api/v1/bookings").param("userId", String.valueOf(CUSTOMER_B))
                        .header("Authorization", TestTokens.bearer(TestTokens.customer(CUSTOMER_A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].userId").value(CUSTOMER_A));
    }

    @Test
    void rejectsInvalidPagingAndSortParameters() throws Exception {
        String bearer = TestTokens.bearer(adminToken);
        mvc.perform(get("/api/v1/bookings").param("size", "101").header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"));
        mvc.perform(get("/api/v1/bookings").param("page", "-1").header("Authorization", bearer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/bookings").param("sort", "userPassword,asc").header("Authorization", bearer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/bookings").param("status", "KHONG_CO").header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Giá trị không hợp lệ cho tham số 'status'"));
        mvc.perform(get("/api/v1/bookings").param("from", at(3, 0, 0).toString()).param("to", at(1, 0, 0).toString())
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFieldsReturnFieldErrors() throws Exception {
        postBooking(TestTokens.customer(CUSTOMER_A), "{\"pitchId\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
                .andExpect(jsonPath("$.errors.pitchId").value("pitchId phải là số dương"))
                .andExpect(jsonPath("$.errors.startTime").value("startTime không được để trống"))
                .andExpect(jsonPath("$.errors.endTime").value("endTime không được để trống"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        postBooking(TestTokens.customer(CUSTOMER_A), "{\"pitchId\":1,\"startTime\":\"ngay mai\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_BAD_REQUEST"));
    }

    @Test
    void businessTimeRulesAreEnforced() throws Exception {
        String token = TestTokens.customer(CUSTOMER_A);
        expectBadRequest(token, bookingJson(1, at(5, 10, 0), at(5, 9, 0)), "endTime phải sau startTime");
        expectBadRequest(token, bookingJson(1, at(5, 23, 0), at(6, 0, 30)),
                "Giờ bắt đầu và kết thúc phải trong cùng một ngày");
        expectBadRequest(token, bookingJson(1, at(-3, 10, 0), at(-3, 11, 0)),
                "startTime phải ở thời điểm trong tương lai");
        expectBadRequest(token, bookingJson(1, at(5, 10, 0), at(5, 10, 15)),
                "Thời lượng đặt sân phải từ 30 phút đến 4 giờ");
        expectBadRequest(token, bookingJson(1, at(5, 8, 0), at(5, 13, 0)),
                "Thời lượng đặt sân phải từ 30 phút đến 4 giờ");
        expectBadRequest(token, bookingJson(1, at(90, 8, 0), at(90, 9, 0)), "Chỉ được đặt trước tối đa 60 ngày");
        assertThat(PITCH.totalHits()).as("request sai không được gọi sang pitchs-service").isEqualTo(8);
    }

    @Test
    void noteLongerThan255CharactersIsRejected() throws Exception {
        String json = "{\"pitchId\":1,\"startTime\":\"" + at(5, 8, 0) + "\",\"endTime\":\"" + at(5, 9, 0)
                + "\",\"note\":\"" + "x".repeat(256) + "\"}";
        postBooking(TestTokens.customer(CUSTOMER_A), json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.note").value("note tối đa 255 ký tự"));
    }

    private void expectBadRequest(String token, String json, String message) throws Exception {
        postBooking(token, json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }
}
