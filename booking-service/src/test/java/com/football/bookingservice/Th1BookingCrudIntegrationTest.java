package com.football.bookingservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.football.bookingservice.entity.Booking;
import com.football.bookingservice.entity.BookingStatus;
import com.football.bookingservice.support.IntegrationTestBase;
import com.football.bookingservice.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH1 - CRUD Booking: đặt, xem, sửa, hủy, xem lịch đặt sân theo thời gian. */
class Th1BookingCrudIntegrationTest extends IntegrationTestBase {

    @Test
    void createBookingReturns201WithLocationAndPriceFromPitchService() throws Exception {
        LocalDateTime start = at(0, 18, 0);
        String json = "{\"pitchId\":1,\"startTime\":\"" + start + "\",\"endTime\":\"" + start.plusMinutes(90)
                + "\",\"note\":\"  Doi bong K65  \"}";

        var result = postBooking(TestTokens.customer(CUSTOMER_A), json)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/bookings/[0-9]+")))
                .andReturn();

        JsonNode data = json(result).get("data");
        assertThat(json(result).get("success").asBoolean()).isTrue();
        assertThat(data.get("userId").asLong()).isEqualTo(CUSTOMER_A);
        assertThat(data.get("pitchName").asText()).isEqualTo("San 1");
        assertThat(data.get("status").asText()).isEqualTo("CHO_DUYET");
        assertThat(data.get("totalPrice").decimalValue()).isEqualByComparingTo("450000.00");
        assertThat(data.get("pricePerHour").decimalValue()).isEqualByComparingTo("300000.00");
        assertThat(data.get("durationHours").asDouble()).isEqualTo(1.5);
        assertThat(data.get("note").asText()).isEqualTo("Doi bong K65");
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void customerSeesOnlyOwnBookingsInMyList() throws Exception {
        createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(CUSTOMER_A, 2, at(1, 8, 0), at(1, 9, 0));
        createBooking(CUSTOMER_B, 1, at(0, 10, 0), at(0, 11, 0));

        getWithToken("/api/v1/bookings/my", TestTokens.customer(CUSTOMER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].userId").value(CUSTOMER_A))
                .andExpect(jsonPath("$.data.content[1].userId").value(CUSTOMER_A));
    }

    @Test
    void getBookingByIdReturnsDetails() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        getWithToken("/api/v1/bookings/" + id, TestTokens.customer(CUSTOMER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.statusDescription").value("Chờ duyệt"));
    }

    @Test
    void getMissingBookingReturns404WithStandardError() throws Exception {
        getWithToken("/api/v1/bookings/999999", TestTokens.admin(ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("ERR_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/999999"));
    }

    @Test
    void updateReschedulesPendingBookingAndRecalculatesPrice() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        mvc.perform(put("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.customer(CUSTOMER_A)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(2, at(2, 19, 0), at(2, 21, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pitchId").value(2))
                .andExpect(jsonPath("$.data.totalPrice").value(1000000.00));

        Booking saved = bookingRepository.findById(id).orElseThrow();
        assertThat(saved.getPitchId()).isEqualTo(2L);
        assertThat(saved.getStartTime()).isEqualTo(at(2, 19, 0));
    }

    @Test
    void updateApprovedBookingIsRejectedWith409() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        approve(id);

        mvc.perform(put("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.customer(CUSTOMER_A)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(1, at(0, 10, 0), at(0, 11, 0))))
                .andExpect(status().isConflict());
        assertThat(PITCH.totalHits()).as("không gọi pitchs-service khi đơn không sửa được nữa").isEqualTo(2);
    }

    @Test
    void cancelIsSoftDeleteAndReleasesSlot() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel")
                        .header("Authorization", TestTokens.bearer(TestTokens.customer(CUSTOMER_A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DA_HUY"));

        assertThat(bookingRepository.findById(id)).get().extracting(Booking::getStatus).isEqualTo(BookingStatus.DA_HUY);
        createBooking(CUSTOMER_B, 1, at(0, 8, 0), at(0, 9, 0));
    }

    @Test
    void deleteReturns204AndKeepsHistory() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));

        mvc.perform(delete("/api/v1/bookings/" + id)
                        .header("Authorization", TestTokens.bearer(TestTokens.customer(CUSTOMER_A))))
                .andExpect(status().isNoContent());

        assertThat(bookingRepository.findById(id)).get().extracting(Booking::getStatus).isEqualTo(BookingStatus.DA_HUY);
    }

    @Test
    void cancellingTwiceIsRejectedWith409() throws Exception {
        long id = createBooking(CUSTOMER_A, 1, at(0, 8, 0), at(0, 9, 0));
        String token = TestTokens.bearer(TestTokens.customer(CUSTOMER_A));
        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel").header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/bookings/" + id + "/cancel").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR_RESOURCE_CONFLICT"));
    }

    @Test
    void overlappingBookingOnSamePitchIsRejectedWith409() throws Exception {
        createBooking(CUSTOMER_A, 1, at(0, 18, 0), at(0, 19, 30));

        postBooking(TestTokens.customer(CUSTOMER_B), bookingJson(1, at(0, 19, 0), at(0, 20, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Khung giờ này đã có người đặt, vui lòng chọn giờ khác"));
    }

    @Test
    void adjacentSlotsAndOtherPitchesDoNotConflict() throws Exception {
        createBooking(CUSTOMER_A, 1, at(0, 18, 0), at(0, 19, 0));

        createBooking(CUSTOMER_B, 1, at(0, 19, 0), at(0, 20, 0));
        createBooking(CUSTOMER_B, 2, at(0, 18, 0), at(0, 19, 0));
        assertThat(bookingRepository.count()).isEqualTo(3);
    }

    @Test
    void publicScheduleListsBusySlotsWithoutPersonalData() throws Exception {
        createBooking(CUSTOMER_A, 1, at(0, 18, 0), at(0, 19, 0));
        long cancelled = createBooking(CUSTOMER_B, 1, at(0, 20, 0), at(0, 21, 0));
        mvc.perform(patch("/api/v1/bookings/" + cancelled + "/cancel")
                .header("Authorization", TestTokens.bearer(TestTokens.customer(CUSTOMER_B))));
        createBooking(CUSTOMER_B, 1, at(1, 18, 0), at(1, 19, 0));

        mvc.perform(get("/api/v1/bookings/pitches/1/schedule").param("date", at(0, 0, 0).toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.busySlots.length()").value(1))
                .andExpect(jsonPath("$.data.busySlots[0].status").value("CHO_DUYET"))
                .andExpect(jsonPath("$.data.busySlots[0].userId").doesNotExist());
    }

    private void approve(long id) throws Exception {
        mvc.perform(patch("/api/v1/bookings/" + id + "/status")
                        .header("Authorization", TestTokens.bearer(TestTokens.owner(OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DA_DUYET\"}"))
                .andExpect(status().isOk());
    }
}
