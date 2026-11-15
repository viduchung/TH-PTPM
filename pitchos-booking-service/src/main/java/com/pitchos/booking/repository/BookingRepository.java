package com.pitchos.booking.repository;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByPitchId(Long pitchId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Booking> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * THUẬT TOÁN KIỂM TRA TRÙNG LỊCH THI ĐẤU (HÙNG PHỤ TRÁCH - CÂU 2 Ý 3)
     * Điều kiện trùng lịch giữa 2 khoảng thời gian [start_new, end_new] và [start_old, end_old]:
     *   b.startTime < :endTime AND b.endTime > :startTime
     * Loại trừ các đơn đã nhả khung giờ: tu_choi (bị từ chối) và da_huy (khách hủy - bổ sung TH1).
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.pitchId = :pitchId " +
           "  AND b.status NOT IN (com.pitchos.booking.entity.BookingStatus.tu_choi, " +
           "                       com.pitchos.booking.entity.BookingStatus.da_huy) " +
           "  AND b.startTime < :endTime " +
           "  AND b.endTime > :startTime")
    Long countOverlappingBookings(@Param("pitchId") Long pitchId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /** Như trên nhưng bỏ qua chính đơn đang được sửa (PUT /bookings/{id}). */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.pitchId = :pitchId " +
           "  AND b.id <> :excludedId " +
           "  AND b.status NOT IN (com.pitchos.booking.entity.BookingStatus.tu_choi, " +
           "                       com.pitchos.booking.entity.BookingStatus.da_huy) " +
           "  AND b.startTime < :endTime " +
           "  AND b.endTime > :startTime")
    Long countOverlappingBookingsExcluding(@Param("pitchId") Long pitchId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("excludedId") Long excludedId);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.pitchId = :pitchId " +
           "  AND b.status NOT IN (com.pitchos.booking.entity.BookingStatus.tu_choi, " +
           "                       com.pitchos.booking.entity.BookingStatus.da_huy) " +
           "  AND b.startTime < :endTime " +
           "  AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(@Param("pitchId") Long pitchId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
