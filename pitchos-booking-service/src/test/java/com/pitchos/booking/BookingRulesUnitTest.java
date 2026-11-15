package com.pitchos.booking;

import com.pitchos.booking.client.WebClientPitchGatewayTestAccess;
import com.pitchos.booking.exception.BadRequestException;
import com.pitchos.booking.service.BookingTimeRules;
import com.pitchos.booking.service.IdempotencyKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit test các quy tắc thuần (không cần Spring): thời gian, tính tiền, Idempotency-Key, phân loại lỗi tạm thời. */
class BookingRulesUnitTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);
    private final BookingTimeRules rules =
            new BookingTimeRules(Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    @Test
    @DisplayName("Khung giờ hợp lệ: tương lai, 30 phút đến 8 giờ")
    void validSlotsPass() {
        assertThatCode(() -> rules.validate(NOW.plusHours(1), NOW.plusHours(1).plusMinutes(30))).doesNotThrowAnyException();
        assertThatCode(() -> rules.validate(NOW.plusHours(1), NOW.plusHours(9))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Biên: bắt đầu đúng bây giờ, 29 phút, 8 giờ 1 phút, end = start → 400")
    void boundarySlotsFail() {
        assertThatThrownBy(() -> rules.validate(NOW, NOW.plusHours(1))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> rules.validate(NOW.plusHours(1), NOW.plusHours(1).plusMinutes(29)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> rules.validate(NOW.plusHours(1), NOW.plusHours(9).plusMinutes(1)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> rules.validate(NOW.plusHours(1), NOW.plusHours(1)))
                .hasMessage("endTime phải sau startTime");
    }

    @Test
    @DisplayName("Tính tiền giữ công thức cũ: giá × phút / 60, làm tròn 2 chữ số")
    void totalPriceFormula() {
        assertThat(BookingTimeRules.totalPrice(new BigDecimal("300000"), NOW, NOW.plusMinutes(90)))
                .isEqualByComparingTo("450000.00");
        assertThat(BookingTimeRules.totalPrice(new BigDecimal("100000"), NOW, NOW.plusMinutes(50)))
                .isEqualByComparingTo("83333.33");
    }

    @Test
    @DisplayName("Idempotency-Key: null được phép; UUID hợp lệ; rỗng, có dấu cách, quá 64 ký tự → 400")
    void idempotencyKeyFormat() {
        assertThat(IdempotencyKeys.normalize(null)).isNull();
        assertThat(IdempotencyKeys.normalize(" 3f1c2b7e-9a1d-4c55-8f0e-2d6b9c1a7e44 "))
                .isEqualTo("3f1c2b7e-9a1d-4c55-8f0e-2d6b9c1a7e44");
        assertThatThrownBy(() -> IdempotencyKeys.normalize("")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> IdempotencyKeys.normalize("a b")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> IdempotencyKeys.normalize("x".repeat(65))).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Retry chỉ cho lỗi tạm thời: lỗi kết nối, 502/503/504; không retry 404, 500, 400")
    void transientErrorClassification() {
        WebClientRequestException connectError = new WebClientRequestException(
                new IOException("Connection refused"), HttpMethod.GET, URI.create("http://pitch"), new HttpHeaders());

        assertThat(WebClientPitchGatewayTestAccess.isTransient(connectError)).isTrue();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(response(503))).isTrue();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(response(502))).isTrue();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(response(504))).isTrue();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(response(500))).isFalse();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(response(404))).isFalse();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(response(400))).isFalse();
        assertThat(WebClientPitchGatewayTestAccess.isTransient(new IllegalStateException())).isFalse();
    }

    private static WebClientResponseException response(int status) {
        return WebClientResponseException.create(status, "status " + status, new HttpHeaders(), new byte[0], null);
    }

}
