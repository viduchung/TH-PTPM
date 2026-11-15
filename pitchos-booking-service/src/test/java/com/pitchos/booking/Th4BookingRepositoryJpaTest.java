package com.pitchos.booking;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TH4 - Tầng JPA trên H2 (chế độ MySQL): mapping Entity, ghi/đọc CSDL, truy vấn trùng lịch, ràng buộc UNIQUE,
 * index và khóa lạc quan. LƯU Ý: đây là H2, KHÔNG phải MySQL thật.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class Th4BookingRepositoryJpaTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2030, 1, 10, 18, 0);

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Lưu và đọc lại đúng mọi cột (logical ID user_id, pitch_id; giá; trạng thái; thời điểm tạo/cập nhật)")
    void saveAndReload_mapsAllColumns() {
        Booking saved = bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusMinutes(90), BookingStatus.cho_duyet));
        entityManager.clear();

        Booking reloaded = bookingRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(3L);
        assertThat(reloaded.getPitchId()).isEqualTo(1L);
        assertThat(reloaded.getStartTime()).isEqualTo(BASE);
        assertThat(reloaded.getEndTime()).isEqualTo(BASE.plusMinutes(90));
        assertThat(reloaded.getTotalPrice()).isEqualByComparingTo("450000.00");
        assertThat(reloaded.getPricePerHour()).isEqualByComparingTo("300000.00");
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.cho_duyet);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("Trùng lịch: chồng giờ → tính; liền kề → không tính; tu_choi và da_huy → bỏ qua; sân khác → không tính")
    void overlapQuery_followsBusinessRules() {
        bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.da_duyet));
        bookingRepository.saveAndFlush(booking(3L, 1L, BASE.plusHours(2), BASE.plusHours(3), BookingStatus.tu_choi));
        bookingRepository.saveAndFlush(booking(3L, 1L, BASE.plusHours(4), BASE.plusHours(5), BookingStatus.da_huy));

        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusMinutes(30), BASE.plusMinutes(90))).isEqualTo(1);
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusHours(1), BASE.plusHours(2))).isZero();
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusHours(2), BASE.plusHours(3))).isZero();
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE.plusHours(4), BASE.plusHours(5))).isZero();
        assertThat(bookingRepository.countOverlappingBookings(2L, BASE, BASE.plusHours(1))).isZero();
        assertThat(bookingRepository.findOverlappingBookings(1L, BASE.minusHours(1), BASE.plusHours(6))).hasSize(1);
    }

    @Test
    @DisplayName("Kiểm tra trùng khi sửa đơn bỏ qua chính đơn đó")
    void overlapExcludingSelf() {
        Booking own = bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet));

        assertThat(bookingRepository.countOverlappingBookingsExcluding(1L, BASE, BASE.plusMinutes(90), own.getId()))
                .isZero();
        assertThat(bookingRepository.countOverlappingBookings(1L, BASE, BASE.plusMinutes(90))).isEqualTo(1);
    }

    @Test
    @DisplayName("UNIQUE (user_id, idempotency_key): cùng khách + cùng key → lỗi; khác khách hoặc key NULL → được")
    void idempotencyUniqueConstraint() {
        Booking first = booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet);
        first.setIdempotencyKey("key-123");
        bookingRepository.saveAndFlush(first);

        Booking otherUserSameKey = booking(4L, 2L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet);
        otherUserSameKey.setIdempotencyKey("key-123");
        bookingRepository.saveAndFlush(otherUserSameKey);
        bookingRepository.saveAndFlush(booking(3L, 2L, BASE.plusHours(2), BASE.plusHours(3), BookingStatus.cho_duyet));
        bookingRepository.saveAndFlush(booking(3L, 2L, BASE.plusHours(4), BASE.plusHours(5), BookingStatus.cho_duyet));

        Booking duplicate = booking(3L, 2L, BASE.plusHours(6), BASE.plusHours(7), BookingStatus.cho_duyet);
        duplicate.setIdempotencyKey("key-123");
        assertThatThrownBy(() -> bookingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Schema có index cho truy vấn trùng lịch / danh sách khách và KHÔNG có khóa ngoại sang DB khác")
    void schemaHasIndexesAndNoForeignKeys() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT LOWER(index_name) FROM information_schema.indexes WHERE LOWER(table_name) = 'bookings'",
                String.class);
        assertThat(indexes).contains("idx_bookings_pitch_time", "idx_bookings_user_created", "idx_bookings_status");

        Integer foreignKeys = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE LOWER(table_name) = 'bookings' AND constraint_type = 'FOREIGN KEY'", Integer.class);
        assertThat(foreignKeys).isZero();
    }

    @Test
    @DisplayName("Khóa lạc quan: version tăng sau mỗi lần cập nhật")
    void versionIncrementsOnUpdate() {
        Booking saved = bookingRepository.saveAndFlush(booking(3L, 1L, BASE, BASE.plusHours(1), BookingStatus.cho_duyet));
        saved.setStatus(BookingStatus.da_duyet);
        Booking updated = bookingRepository.saveAndFlush(saved);
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    private static Booking booking(Long userId, Long pitchId, LocalDateTime start, LocalDateTime end,
                                   BookingStatus status) {
        return Booking.builder()
                .userId(userId)
                .pitchId(pitchId)
                .startTime(start)
                .endTime(end)
                .pricePerHour(new BigDecimal("300000.00"))
                .totalPrice(new BigDecimal("450000.00"))
                .status(status)
                .build();
    }
}
