package com.football.bookingservice.repository;

import com.football.bookingservice.entity.Booking;
import com.football.bookingservice.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /**
     * Thuật toán chống trùng lịch (data-layer, TH1): hai khoảng [s1, e1) và [s2, e2) giao nhau khi
     * s1 < e2 AND e1 > s2. Bỏ qua đơn đã nhả khung giờ (TU_CHOI, DA_HUY) và đơn đang được sửa (excludedId).
     * Dùng chỉ mục idx_bookings_pitch_time (pitch_id, start_time, end_time).
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.pitchId = :pitchId
              AND b.status NOT IN :releasedStatuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
              AND (:excludedId IS NULL OR b.id <> :excludedId)
            """)
    long countOverlapping(@Param("pitchId") Long pitchId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("excludedId") Long excludedId,
                          @Param("releasedStatuses") Collection<BookingStatus> releasedStatuses);

    /** Lịch đã kín của một sân trong một khoảng thời gian (TH1: xem lịch đặt sân theo thời gian). */
    List<Booking> findByPitchIdAndStatusNotInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
            Long pitchId, Collection<BookingStatus> releasedStatuses, LocalDateTime rangeEnd, LocalDateTime rangeStart);

    /** Khóa dòng đơn khi đổi trạng thái: duyệt và hủy cùng lúc thì thao tác sau thấy trạng thái mới. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
