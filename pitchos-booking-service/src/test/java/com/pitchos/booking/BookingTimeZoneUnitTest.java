package com.pitchos.booking;

import com.pitchos.booking.config.TimeConfig;
import com.pitchos.booking.exception.BadRequestException;
import com.pitchos.booking.service.BookingTimeRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Múi giờ nghiệp vụ phải là Asia/Ho_Chi_Minh, KHÔNG phụ thuộc múi giờ mặc định của máy / Docker container
 * (container thường để UTC → nếu dùng múi giờ máy, quy tắc "giờ bắt đầu phải ở tương lai" lệch 7 giờ).
 */
class BookingTimeZoneUnitTest {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private TimeZone originalDefault;

    @BeforeEach
    void rememberDefault() {
        originalDefault = TimeZone.getDefault();
    }

    @AfterEach
    void restoreDefault() {
        TimeZone.setDefault(originalDefault);
    }

    @ParameterizedTest(name = "múi giờ mặc định của JVM = {0}")
    @ValueSource(strings = {"UTC", "America/New_York", "Asia/Tokyo", "Asia/Ho_Chi_Minh"})
    @DisplayName("Clock của ứng dụng luôn theo giờ Việt Nam, dù múi giờ mặc định của máy là gì")
    void applicationClockAlwaysUsesVietnamZone(String jvmDefaultZone) {
        TimeZone.setDefault(TimeZone.getTimeZone(jvmDefaultZone));

        assertThat(new TimeConfig().clock().getZone()).isEqualTo(VIETNAM);
    }

    /** 03:00 UTC = 10:00 giờ Việt Nam. */
    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");
    private static final LocalDate TODAY_VN = LocalDate.of(2026, 11, 15);

    @ParameterizedTest(name = "múi giờ mặc định của JVM = {0}")
    @ValueSource(strings = {"UTC", "America/New_York", "Asia/Tokyo", "Asia/Ho_Chi_Minh"})
    @DisplayName("Cùng một thời điểm (10:00 giờ VN): quy tắc 'giờ bắt đầu ở tương lai' cho cùng kết quả ở mọi múi giờ máy")
    void startTimeRuleFollowsVietnamTimeRegardlessOfJvmDefault(String jvmDefaultZone) {
        TimeZone.setDefault(TimeZone.getTimeZone(jvmDefaultZone));
        // Đồng hồ hệ thống của máy (theo múi giờ mặc định) → đồng hồ nghiệp vụ của ứng dụng
        BookingTimeRules rules = new BookingTimeRules(
                TimeConfig.businessClock(Clock.fixed(NOW, ZoneId.systemDefault())));

        assertThatCode(() -> rules.validate(TODAY_VN.atTime(10, 30), TODAY_VN.atTime(11, 30)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.validate(TODAY_VN.atTime(10, 0), TODAY_VN.atTime(11, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startTime phải ở thời điểm trong tương lai");
        assertThatThrownBy(() -> rules.validate(TODAY_VN.atTime(9, 30), TODAY_VN.atTime(10, 30)))
                .isInstanceOf(BadRequestException.class);
        assertThat(rules.hasStarted(TODAY_VN.atTime(9, 59))).isTrue();
        assertThat(rules.hasStarted(TODAY_VN.atTime(10, 1))).isFalse();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Đối chứng lỗi cũ: dùng thẳng đồng hồ máy UTC thì 09:30 giờ VN (đã qua) bị coi là tương lai")
    void rawUtcClockWouldAcceptPastVietnamTime() {
        BookingTimeRules rawUtcRules = new BookingTimeRules(Clock.fixed(NOW, ZoneId.of("UTC")));

        assertThatCode(() -> rawUtcRules.validate(TODAY_VN.atTime(9, 30), TODAY_VN.atTime(10, 30)))
                .doesNotThrowAnyException();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Khi khởi động, múi giờ mặc định của JVM được đặt về Asia/Ho_Chi_Minh")
    void applyBusinessTimeZoneSetsJvmDefault() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        TimeConfig.applyBusinessTimeZoneAsDefault();

        assertThat(ZoneId.systemDefault()).isEqualTo(VIETNAM);
    }
}
