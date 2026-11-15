package com.pitchos.booking;

import com.pitchos.booking.support.IntegrationTestBase;
import com.pitchos.booking.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TH3 - REST nâng cao: phân trang, sắp xếp, lọc theo trạng thái / sân / ngày, validation, response lỗi thống nhất. */
class Th3PagingFilterValidationIntegrationTest extends IntegrationTestBase {

    private static final String LIST = "/api/v1/bookings";

    @Test
    @DisplayName("Phân trang: page/size/totalElements/totalPages đúng, sắp xếp theo startTime tăng dần")
    void paging_returnsRequestedPage() throws Exception {
        for (int hour = 6; hour < 11; hour++) {
            createBooking(USER_A, 1, at(0, hour, 0), at(0, hour + 1, 0));
        }

        getWithToken(LIST + "?page=1&size=2&sort=startTime,asc", TestTokens.user(USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].startTime").value(at(0, 8, 0).toString() + ":00"));
    }

    @Test
    @DisplayName("Lọc theo trạng thái, theo sân và theo ngày (date=yyyy-MM-dd)")
    void filters_byStatusPitchAndDate() throws Exception {
        long day0Pitch1 = createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_A, 2, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_A, 1, at(1, 8, 0), at(1, 9, 0));
        mvc.perform(patch(LIST + "/" + day0Pitch1 + "/status")
                .header("Authorization", TestTokens.bearer(TestTokens.admin(ADMIN)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"da_duyet\"}"))
                .andExpect(status().isOk());

        getWithToken(LIST + "?status=da_duyet", TestTokens.user(USER_A))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(day0Pitch1));
        getWithToken(LIST + "?pitchId=1", TestTokens.user(USER_A))
                .andExpect(jsonPath("$.totalElements").value(2));
        getWithToken(LIST + "?date=" + at(1, 0, 0).toLocalDate(), TestTokens.user(USER_A))
                .andExpect(jsonPath("$.totalElements").value(1));
        getWithToken(LIST + "?pitchId=1&date=" + at(0, 0, 0).toLocalDate(), TestTokens.user(USER_A))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Lọc theo khoảng from/to (theo giờ bắt đầu đá: from ≤ startTime < to)")
    void filters_byTimeRange() throws Exception {
        createBooking(USER_A, 1, at(0, 8, 0), at(0, 9, 0));
        createBooking(USER_A, 1, at(0, 10, 0), at(0, 11, 0));
        createBooking(USER_A, 1, at(0, 12, 0), at(0, 13, 0));

        getWithToken(LIST + "?from=" + at(0, 9, 0) + "&to=" + at(0, 12, 0), TestTokens.user(USER_A))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Tham số danh sách sai → 400 thống nhất: size > 100, page âm, sort lạ, status lạ, from ≥ to, date + from")
    void invalidListParameters_return400() throws Exception {
        String token = TestTokens.user(USER_A);
        getWithToken(LIST + "?size=101", token).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(org.hamcrest.Matchers.startsWith("size"))));
        getWithToken(LIST + "?page=-1", token).andExpect(status().isBadRequest());
        getWithToken(LIST + "?sort=password,asc", token).andExpect(status().isBadRequest());
        getWithToken(LIST + "?sort=startTime,sideways", token).andExpect(status().isBadRequest());
        getWithToken(LIST + "?status=khong_ton_tai", token).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Giá trị của tham số 'status' không hợp lệ"));
        getWithToken(LIST + "?from=" + at(0, 12, 0) + "&to=" + at(0, 9, 0), token).andExpect(status().isBadRequest());
        getWithToken(LIST + "?date=" + at(0, 0, 0).toLocalDate() + "&from=" + at(0, 9, 0), token)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation body: thiếu trường → 400 kèm details từng trường; pitchId âm → 400")
    void bodyValidation_listsAllFieldErrors() throws Exception {
        postBooking(TestTokens.user(USER_A), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details", hasSize(3)));
        postBooking(TestTokens.user(USER_A), bookingJson(-1, at(0, 8, 0), at(0, 9, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pitchId phải là số dương"));
        postBooking(TestTokens.user(USER_A), "{\"pitchId\": \"abc\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không đúng định dạng"));
        assertThat(PITCH.hits()).isZero();
    }

    @Test
    @DisplayName("Validation ngày giờ: end ≤ start, giờ quá khứ, ngắn hơn 30 phút, dài hơn 8 giờ → 400, không gọi Pitch")
    void timeValidation_rejectsInvalidSlots() throws Exception {
        String token = TestTokens.user(USER_A);
        LocalDateTime past = LocalDateTime.now().minusDays(1).withNano(0);

        postBooking(token, bookingJson(1, at(0, 10, 0), at(0, 9, 0))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endTime phải sau startTime"));
        postBooking(token, bookingJson(1, past, past.plusHours(1))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTime phải ở thời điểm trong tương lai"));
        postBooking(token, bookingJson(1, at(0, 10, 0), at(0, 10, 15))).andExpect(status().isBadRequest());
        postBooking(token, bookingJson(1, at(0, 6, 0), at(0, 15, 0))).andExpect(status().isBadRequest());

        assertThat(PITCH.hits()).isZero();
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    @DisplayName("Lỗi phía client do Spring sinh ra (sai Content-Type 415, sai method 405) không bị biến thành 500")
    void springClientErrors_keepTheirStatus() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(LIST)
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A)))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("pitchId=1"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(LIST)
                        .header("Authorization", TestTokens.bearer(TestTokens.user(USER_A))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        assertThat(PITCH.hits()).isZero();
    }

    @Test
    @DisplayName("Đường dẫn không tồn tại / id sai kiểu → 404 / 400 cùng định dạng lỗi")
    void unknownPathAndBadId_useUnifiedErrorFormat() throws Exception {
        getWithToken(LIST + "/abc", TestTokens.user(USER_A))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value(LIST + "/abc"));
        getWithToken("/api/v1/khong-ton-tai", TestTokens.user(USER_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
