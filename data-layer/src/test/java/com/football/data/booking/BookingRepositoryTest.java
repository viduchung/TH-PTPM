package com.football.data.booking;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    private final Long PITCH_1 = 100L;
    private final Long PITCH_2 = 200L;
    private final Long USER_1 = 1L;
    private final Long USER_2 = 2L;

    // Lịch đã tồn tại trên PITCH_1: từ 18:00 đến 19:30 ngày 20/11/2026
    private final LocalDateTime BASE_START = LocalDateTime.of(2026, 11, 20, 18, 0);
    private final LocalDateTime BASE_END   = LocalDateTime.of(2026, 11, 20, 19, 30);

    private Booking existingBooking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();

        existingBooking = Booking.builder()
                .userId(USER_1)
                .pitchId(PITCH_1)
                .startTime(BASE_START)
                .endTime(BASE_END)
                .totalPrice(new BigDecimal("450000.00"))
                .pricePerHour(new BigDecimal("300000.00"))
                .status(BookingStatus.DA_DUYET)
                .note("Đơn đã duyệt trước")
                .build();

        existingBooking = bookingRepository.save(existingBooking);
    }

    @Test
    @DisplayName("1. Lưu và lấy thông tin đơn đặt sân thành công (Khóa logic userId & pitchId)")
    void testSaveAndFindBooking() {
        List<Booking> list = bookingRepository.findByUserId(USER_1);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPitchId()).isEqualTo(PITCH_1);
        assertThat(list.get(0).getStatus()).isEqualTo(BookingStatus.DA_DUYET);
    }

    @Test
    @DisplayName("2. Trùng lấn hoàn toàn: Khung giờ mới trùng khớp 100% (18:00 - 19:30)")
    void testExactTimeOverlap() {
        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, BASE_START, BASE_END);
        assertThat(conflicts).isEqualTo(1L);
    }

    @Test
    @DisplayName("3. Trùng giao thoa đầu: Khung giờ mới bắt đầu sớm hơn và lấn vào (17:30 - 18:30)")
    void testPartialOverlapStart() {
        LocalDateTime newStart = LocalDateTime.of(2026, 11, 20, 17, 30);
        LocalDateTime newEnd   = LocalDateTime.of(2026, 11, 20, 18, 30);

        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, newStart, newEnd);
        assertThat(conflicts).isEqualTo(1L);
    }

    @Test
    @DisplayName("4. Trùng giao thoa cuối: Khung giờ mới bắt đầu trong giờ và kết thúc muộn hơn (19:00 - 20:30)")
    void testPartialOverlapEnd() {
        LocalDateTime newStart = LocalDateTime.of(2026, 11, 20, 19, 0);
        LocalDateTime newEnd   = LocalDateTime.of(2026, 11, 20, 20, 30);

        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, newStart, newEnd);
        assertThat(conflicts).isEqualTo(1L);
    }

    @Test
    @DisplayName("5. Trùm toàn bộ: Khung giờ mới bao trọn cả khung giờ cũ (17:00 - 20:00)")
    void testEnclosingOverlap() {
        LocalDateTime newStart = LocalDateTime.of(2026, 11, 20, 17, 0);
        LocalDateTime newEnd   = LocalDateTime.of(2026, 11, 20, 20, 0);

        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, newStart, newEnd);
        assertThat(conflicts).isEqualTo(1L);
    }

    @Test
    @DisplayName("6. Nằm lọt bên trong: Khung giờ mới nằm trọn trong khung giờ cũ (18:15 - 19:15)")
    void testInsideOverlap() {
        LocalDateTime newStart = LocalDateTime.of(2026, 11, 20, 18, 15);
        LocalDateTime newEnd   = LocalDateTime.of(2026, 11, 20, 19, 15);

        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, newStart, newEnd);
        assertThat(conflicts).isEqualTo(1L);
    }

    @Test
    @DisplayName("7. Liền kề biên trước: Kết thúc đúng lúc giờ cũ bắt đầu (16:30 - 18:00) -> KHÔNG TRÙNG")
    void testAdjacentBefore() {
        LocalDateTime newStart = LocalDateTime.of(2026, 11, 20, 16, 30);
        LocalDateTime newEnd   = LocalDateTime.of(2026, 11, 20, 18, 0);

        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, newStart, newEnd);
        assertThat(conflicts).isEqualTo(0L);
    }

    @Test
    @DisplayName("8. Liền kề biên sau: Bắt đầu đúng lúc giờ cũ kết thúc (19:30 - 21:00) -> KHÔNG TRÙNG")
    void testAdjacentAfter() {
        LocalDateTime newStart = LocalDateTime.of(2026, 11, 20, 19, 30);
        LocalDateTime newEnd   = LocalDateTime.of(2026, 11, 20, 21, 0);

        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_1, newStart, newEnd);
        assertThat(conflicts).isEqualTo(0L);
    }

    @Test
    @DisplayName("9. Khác sân: Cùng khung giờ nhưng trên sân khác (PITCH_2) -> KHÔNG TRÙNG")
    void testDifferentPitchNoConflict() {
        Long conflicts = bookingRepository.countOverlappingBookings(PITCH_2, BASE_START, BASE_END);
        assertThat(conflicts).isEqualTo(0L);
    }

    @Test
    @DisplayName("10. Đơn bị từ chối hoặc đã hủy: Sân được giải phóng -> KHÔNG BỊ TRÙNG")
    void testRejectedOrCancelledDoesNotBlockSlot() {
        // Đổi trạng thái đơn hiện tại thành TU_CHOI
        existingBooking.setStatus(BookingStatus.TU_CHOI);
        bookingRepository.save(existingBooking);

        Long conflicts1 = bookingRepository.countOverlappingBookings(PITCH_1, BASE_START, BASE_END);
        assertThat(conflicts1).isEqualTo(0L);

        // Đổi trạng thái đơn thành DA_HUY
        existingBooking.setStatus(BookingStatus.DA_HUY);
        bookingRepository.save(existingBooking);

        Long conflicts2 = bookingRepository.countOverlappingBookings(PITCH_1, BASE_START, BASE_END);
        assertThat(conflicts2).isEqualTo(0L);
    }

    @Test
    @DisplayName("11. Cập nhật đơn: Bỏ qua chính ID đơn đang sửa -> countOverlappingBookingsExcluding")
    void testExcludingSelfWhenUpdating() {
        Long conflicts = bookingRepository.countOverlappingBookingsExcluding(
                PITCH_1, BASE_START, BASE_END, existingBooking.getId());
        assertThat(conflicts).isEqualTo(0L);
    }
}

