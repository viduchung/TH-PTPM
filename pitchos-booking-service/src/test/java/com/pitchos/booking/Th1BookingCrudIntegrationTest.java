package com.pitchos.booking;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH1 - CRUD và RESTful API của Booking: đặt, xem, sửa, hủy, duyệt, từ chối. */
class Th1BookingCrudIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST đặt sân hợp lệ → 201, Location, tính tiền theo giá Pitch Service, lưu cho_duyet vào CSDL")
    void createBooking_returnsCreatedAndPersists() throws Exception {
        LocalDateTime start = at(0, 18, 0);
        LocalDateTime end = at(0, 19, 30);

        String body = postBooking(TestTokens.user(USER_A), bookingJson(1, start, end))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/bookings/")))
                .andExpect(jsonPath("$.userId").value(USER_A))
                .andExpect(jsonPath("$.pitchId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(450000.00))
                .andExpect(jsonPath("$.pricePerHour").value(300000.00))
                .andExpect(jsonPath("$.status").value("cho_duyet"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();
        Booking saved = bookingRepository.findById(id).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("450000.00");
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.cho_duyet);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("GET /my-bookings (API cũ) trả đúng đơn của khách trong token, dạng danh sách")
    void myBookings_returnsOnlyOwnBookings() throws Exception {
        createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_A, 2, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_B, 1, at(1, 8, 0), at(1, 9, 0));

        getWithToken("/api/v1/bookings/my-bookings", TestTokens.user(USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(USER_A))
                .andExpect(jsonPath("$[1].userId").value(USER_A));
    }

    @Test
    @DisplayName("GET /{id} của chính mình → 200; id không tồn tại → 404")
    void getBookingById_ownAndMissing() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 10, 0), at(0, 11, 0));

        getWithToken("/api/v1/bookings/" + id, TestTokens.user(USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        getWithToken("/api/v1/bookings/999999", TestTokens.user(USER_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/999999"));
    }

    @Test
    @DisplayName("PUT /{id} đổi sân và giờ của đơn cho_duyet → 200, tính lại tiền theo giá sân mới")
    void updateBooking_reschedulesAndRecalculatesPrice() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 10, 0), at(0, 11, 0));

        mvc.perform(put("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(2, at(0, 14, 0), at(0, 16, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pitchId").value(2))
                .andExpect(jsonPath("$.totalPrice").value(1000000.00))
                .andExpect(jsonPath("$.status").value("cho_duyet"));

        Booking updated = bookingRepository.findById(id).orElseThrow();
        assertThat(updated.getPitchId()).isEqualTo(2L);
        assertThat(updated.getStartTime()).isEqualTo(at(0, 14, 0));
        assertThat(updated.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("PUT đơn đã duyệt → 400 (chỉ sửa được đơn cho_duyet)")
    void updateBooking_rejectedWhenNotPending() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 10, 0), at(0, 11, 0));
        approve(id, "da_duyet").andExpect(status().isOk());

        mvc.perform(put("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(1, at(0, 12, 0), at(0, 13, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /{id}/cancel → da_huy, đơn vẫn còn trong CSDL và khung giờ được nhả cho người khác")
    void cancelBooking_softDeletesAndReleasesSlot() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 18, 0), at(0, 19, 0));

        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel")
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("da_huy"));

        assertThat(bookingRepository.findById(id)).get()
                .extracting(Booking::getStatus).isEqualTo(BookingStatus.da_huy);
        postBooking(TestTokens.user(USER_B), bookingJson(1, at(0, 18, 0), at(0, 19, 0)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /{id} → 204, hủy mềm (giữ lịch sử); hủy lại lần nữa → 400")
    void deleteBooking_isSoftCancel() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 18, 0), at(0, 19, 0));

        mvc.perform(delete("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A))))
                .andExpect(status().isNoContent());
        assertThat(bookingRepository.findById(id)).get()
                .extracting(Booking::getStatus).isEqualTo(BookingStatus.da_huy);

        mvc.perform(delete("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Chủ sân duyệt (da_duyet) và từ chối (tu_choi); đơn tu_choi không đổi trạng thái được nữa")
    void approveAndReject_followStatusRules() throws Exception {
        long approvedId = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        long rejectedId = createBooking(USER_A, 1, at(0, 10, 0), at(0, 11, 0));

        approve(approvedId, "da_duyet").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("da_duyet"));
        approve(rejectedId, "tu_choi").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("tu_choi"));
        approve(rejectedId, "da_duyet").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH status chỉ nhận da_duyet / tu_choi: cho_duyet, da_huy → 400; đơn không tồn tại → 404")
    void updateStatus_rejectsInvalidTargets() throws Exception {
        long id = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));

        approve(id, "cho_duyet").andExpect(status().isBadRequest());
        approve(id, "da_huy").andExpect(status().isBadRequest());
        approve(999999L, "da_duyet").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Đặt trùng khung giờ → 400; khung giờ liền kề (kết thúc = bắt đầu) vẫn đặt được")
    void overlapIsRejected_butAdjacentSlotAllowed() throws Exception {
        createBooking(USER_A, 1, at(0, 18, 0), at(0, 19, 0));

        postBooking(TestTokens.user(USER_B), bookingJson(1, at(0, 18, 30), at(0, 19, 30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Khung giờ này đã có người đặt"));
        postBooking(TestTokens.user(USER_B), bookingJson(1, at(0, 19, 0), at(0, 20, 0)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(String.valueOf(
                        bookingRepository.findAll().stream().mapToLong(Booking::getId).max().orElseThrow()))));
    }

    private org.springframework.test.web.servlet.ResultActions approve(long id, String status) throws Exception {
        return mvc.perform(patch("/api/v1/bookings/" + id + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.owner(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }
}
