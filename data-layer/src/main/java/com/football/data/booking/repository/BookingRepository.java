package com.football.data.booking.repository;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository truy xuất dữ liệu Đơn đặt sân - Spring Data JPA
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 * 
 * Chứa thuật toán và câu truy vấn chống trùng lịch thi đấu độc quyền của Hùng.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByPitchId(Long pitchId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * THUẬT TOÁN KIỂM TRA TRÙNG LỊCH THI ĐẤU (HÙNG PHỤ TRÁCH - CÂU 2 Ý 3 & TUẦN 1)
     * Điều kiện toán học 2 khoảng thời gian [S1, E1] và [S2, E2] giao thoa nhau:
     *   b.startTime < :endTime AND b.endTime > :startTime
     * Bỏ qua các đơn có trạng thái TU_CHOI hoặc DA_HUY (khung giờ đã được giải phóng).
     * 
     * @param pitchId Mã sân bóng
     * @param startTime Thời gian bắt đầu dự kiến
     * @param endTime Thời gian kết thúc dự kiến
     * @return Số lượng đơn đặt bị xung đột (> 0: Bị trùng, = 0: Hợp lệ)
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.pitchId = :pitchId " +
           "  AND b.status NOT IN (com.football.data.booking.entity.BookingStatus.TU_CHOI, " +
           "                       com.football.data.booking.entity.BookingStatus.DA_HUY) " +
           "  AND b.startTime < :endTime " +
           "  AND b.endTime > :startTime")
    Long countOverlappingBookings(@Param("pitchId") Long pitchId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * Kiểm tra trùng lịch ngoại trừ một đơn đặt cụ thể (phục vụ cập nhật/sửa đơn)
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.pitchId = :pitchId " +
           "  AND b.id <> :excludedId " +
           "  AND b.status NOT IN (com.football.data.booking.entity.BookingStatus.TU_CHOI, " +
           "                       com.football.data.booking.entity.BookingStatus.DA_HUY) " +
           "  AND b.startTime < :endTime " +
           "  AND b.endTime > :startTime")
    Long countOverlappingBookingsExcluding(@Param("pitchId") Long pitchId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("excludedId") Long excludedId);

    /**
     * Lấy danh sách các đơn trùng lịch để hiển thị cảnh báo chi tiết
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.pitchId = :pitchId " +
           "  AND b.status NOT IN (com.football.data.booking.entity.BookingStatus.TU_CHOI, " +
           "                       com.football.data.booking.entity.BookingStatus.DA_HUY) " +
           "  AND b.startTime < :endTime " +
           "  AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(@Param("pitchId") Long pitchId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}

