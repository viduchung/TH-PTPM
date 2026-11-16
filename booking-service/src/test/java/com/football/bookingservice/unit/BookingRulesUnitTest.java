package com.football.bookingservice.unit;

import com.football.bookingservice.entity.BookingStatus;
import com.football.bookingservice.exception.BadRequestException;
import com.football.bookingservice.security.AppRole;
import com.football.bookingservice.security.AuthenticatedUser;
import com.football.bookingservice.security.JwtTokenParser;
import com.football.bookingservice.service.BookingTimeRules;
import com.football.bookingservice.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingRulesUnitTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** "Bây giờ" cố định: 2026-11-16 10:00 giờ Việt Nam. */
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-11-16T03:00:00Z"), ZONE);
    private final BookingTimeRules rules = new BookingTimeRules(FIXED);

    @Test
    void acceptsValidSlot() {
        assertThatCode(() -> rules.validate(LocalDateTime.of(2026, 11, 16, 18, 0),
                LocalDateTime.of(2026, 11, 16, 19, 30))).doesNotThrowAnyException();
    }

    @Test
    void rejectsSlotThatAlreadyStartedInVietnamTime() {
        assertThatThrownBy(() -> rules.validate(LocalDateTime.of(2026, 11, 16, 9, 30),
                LocalDateTime.of(2026, 11, 16, 11, 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startTime phải ở thời điểm trong tương lai");
        assertThat(rules.hasStarted(LocalDateTime.of(2026, 11, 16, 10, 0))).isTrue();
        assertThat(rules.hasStarted(LocalDateTime.of(2026, 11, 16, 10, 1))).isFalse();
    }

    @Test
    void acceptsBoundaryDurations() {
        assertThatCode(() -> rules.validate(LocalDateTime.of(2026, 11, 17, 8, 0),
                LocalDateTime.of(2026, 11, 17, 8, 30))).doesNotThrowAnyException();
        assertThatCode(() -> rules.validate(LocalDateTime.of(2026, 11, 17, 8, 0),
                LocalDateTime.of(2026, 11, 17, 12, 0))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "CHO_DUYET, DA_DUYET, true", "CHO_DUYET, TU_CHOI, true", "CHO_DUYET, DA_HUY, true",
            "DA_DUYET, DA_HUY, true", "DA_DUYET, TU_CHOI, false", "DA_DUYET, CHO_DUYET, false",
            "TU_CHOI, DA_DUYET, false", "DA_HUY, CHO_DUYET, false", "DA_HUY, DA_HUY, false"})
    void statusTransitionsFollowDataLayerStateMachine(BookingStatus from, BookingStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void onlyRejectedAndCancelledReleaseTheSlot() {
        assertThat(BookingStatus.CHO_DUYET.occupiesPitchSlot()).isTrue();
        assertThat(BookingStatus.DA_DUYET.occupiesPitchSlot()).isTrue();
        assertThat(BookingStatus.TU_CHOI.occupiesPitchSlot()).isFalse();
        assertThat(BookingStatus.DA_HUY.occupiesPitchSlot()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"ROLE_CUSTOMER, CUSTOMER", "customer, CUSTOMER", "ROLE_FIELD_OWNER, OWNER", "OWNER, OWNER",
            "ROLE_ADMIN, ADMIN", "ROLE_INTERNAL_SERVICE, INTERNAL_SERVICE"})
    void normalizesRoleNamesUsedAcrossTheTeam(String claim, AppRole expected) {
        assertThat(AppRole.fromClaim(claim)).contains(expected);
    }

    @Test
    void unknownRolesMapToNothing() {
        assertThat(AppRole.fromClaim("ROLE_SUPERUSER")).isEmpty();
        assertThat(AppRole.fromClaim(" ")).isEmpty();
        assertThat(AppRole.fromClaim(null)).isEmpty();
    }

    @Test
    void parsesAuthServiceTokenWithoutUserId() {
        JwtTokenParser parser = new JwtTokenParser(TestTokens.SHARED_SECRET);

        AuthenticatedUser user = parser.parse(TestTokens.authStyle("customer01", "ROLE_CUSTOMER")).orElseThrow();

        assertThat(user.username()).isEqualTo("customer01");
        assertThat(user.userId()).isNull();
        assertThat(user.role()).isEqualTo(AppRole.CUSTOMER);
        assertThat(user.toString()).doesNotContain(user.bearerToken());
    }

    @Test
    void parsesDataLayerTokenWithUserIdAndRolesList() {
        JwtTokenParser parser = new JwtTokenParser(TestTokens.SHARED_SECRET);

        AuthenticatedUser user = parser.parse(TestTokens.owner(7)).orElseThrow();

        assertThat(user.userId()).isEqualTo(7L);
        assertThat(user.role()).isEqualTo(AppRole.OWNER);
    }

    @Test
    void rejectsTamperedExpiredAndGarbageTokens() {
        JwtTokenParser parser = new JwtTokenParser(TestTokens.SHARED_SECRET);

        assertThat(parser.parse(TestTokens.signedWithWrongKey(1))).isEmpty();
        assertThat(parser.parse(TestTokens.expiredCustomer(1))).isEmpty();
        assertThat(parser.parse("abc.def.ghi")).isEmpty();
        assertThat(parser.parse("")).isEmpty();
    }
}
