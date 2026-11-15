package com.pitchos.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
        indexes = {
                // Truy vấn trùng lịch: WHERE pitch_id = ? AND start_time < ? AND end_time > ?
                @Index(name = "idx_bookings_pitch_time", columnList = "pitch_id, start_time, end_time"),
                // Danh sách đơn của khách (my-bookings) sắp theo ngày tạo
                @Index(name = "idx_bookings_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_bookings_status", columnList = "status")
        },
        uniqueConstraints = {
                // TH6 - Idempotency: một khách không thể tạo 2 đơn với cùng một Idempotency-Key.
                // MySQL/H2 cho phép nhiều dòng NULL nên các đơn cũ / không gửi key không bị ảnh hưởng.
                @UniqueConstraint(name = "uk_bookings_user_idempotency", columnNames = {"user_id", "idempotency_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NGUYÊN TẮC DATABASE-PER-SERVICE:
     * Lưu Logical ID dạng số nguyên (BIGINT), tuyệt đối KHÔNG tạo Foreign Key vật lý sang pitchos_user_db.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * NGUYÊN TẮC DATABASE-PER-SERVICE:
     * Lưu Logical ID dạng số nguyên (BIGINT), tuyệt đối KHÔNG tạo Foreign Key vật lý sang pitchos_pitch_db.
     */
    @Column(name = "pitch_id", nullable = false)
    private Long pitchId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /** Đơn giá sân tại thời điểm đặt (bản chụp từ Pitch Service), để giá đơn cũ không đổi khi sân đổi giá. */
    @Column(name = "price_per_hour", precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.cho_duyet;

    /** Khóa chống tạo trùng do khách gửi lại request (header Idempotency-Key). */
    @Column(name = "idempotency_key", length = IDEMPOTENCY_KEY_MAX_LENGTH)
    private String idempotencyKey;

    /** Khóa lạc quan: hai thao tác đổi trạng thái cùng lúc (duyệt vs hủy) thì thao tác sau nhận 409. */
    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
