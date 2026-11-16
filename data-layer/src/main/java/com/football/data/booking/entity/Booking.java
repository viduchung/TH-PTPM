package com.football.data.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Đơn đặt sân (Booking) - Quản lý trong cơ sở dữ liệu `booking_db`.
 * Ánh xạ tới bảng `bookings`.
 * 
 * =========================================================================
 * NGUYÊN TẮC DATABASE-PER-SERVICE (HÙNG PHỤ TRÁCH - CÂU 1 Ý 2b & CÂU 2 Ý 3)
 * =========================================================================
 * 1. Lưu `userId` dưới dạng số nguyên (BIGINT) - Khóa Logic (Logical ID).
 * 2. Lưu `pitchId` dưới dạng số nguyên (BIGINT) - Khóa Logic (Logical ID).
 * 3. TUYỆT ĐỐI KHÔNG DÙNG @ManyToOne hay Foreign Key vật lý sang `users` hoặc `pitches`.
 *    Điều này đảm bảo tính đóng gói (Encapsulation), không bị Schema Coupling,
 *    và cho phép Booking Service hoạt động, deploy, scale độc lập hoàn toàn.
 * 4. Đánh chỉ mục Index tổ hợp (pitch_id, start_time, end_time) để câu truy vấn
 *    chống trùng lịch thi đấu đạt hiệu năng O(log N) thay vì Table Scan.
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                // Tối ưu hóa truy vấn trùng lịch: WHERE pitch_id = ? AND start_time < ? AND end_time > ?
                @Index(name = "idx_bookings_pitch_time", columnList = "pitch_id, start_time, end_time"),
                // Tối ưu hóa truy vấn lịch sử đơn đặt của khách hàng
                @Index(name = "idx_bookings_user_id", columnList = "user_id"),
                // Tối ưu hóa lọc theo trạng thái duyệt đơn
                @Index(name = "idx_bookings_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Khóa Logic trỏ sang auth_db (bảng users)
     * Tuyệt đối không tạo Foreign Key vật lý!
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Khóa Logic trỏ sang pitch_db (bảng pitches)
     * Tuyệt đối không tạo Foreign Key vật lý!
     */
    @Column(name = "pitch_id", nullable = false)
    private Long pitchId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /** Đơn giá sân chốt tại thời điểm đặt */
    @Column(name = "price_per_hour", precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.CHO_DUYET;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = BookingStatus.CHO_DUYET;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Booking(Long userId, Long pitchId, LocalDateTime startTime, LocalDateTime endTime, BigDecimal totalPrice, BigDecimal pricePerHour, BookingStatus status, String note) {
        this.userId = userId;
        this.pitchId = pitchId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.pricePerHour = pricePerHour;
        this.status = (status != null) ? status : BookingStatus.CHO_DUYET;
        this.note = note;
    }
}

