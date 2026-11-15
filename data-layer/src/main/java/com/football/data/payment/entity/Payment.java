package com.football.data.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Giao dịch thanh toán (Payment) - Quản lý trong cơ sở dữ liệu `payment_db`
 * Ánh xạ tới bảng `payments`.
 * 
 * NGUYÊN TẮC DATABASE-PER-SERVICE:
 * - `bookingId` lưu dạng số nguyên (BIGINT) - Khóa Logic (Logical ID).
 * - Tuyệt đối không tạo Foreign Key vật lý sang booking_db.
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 4 (TH4)
 */
@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_transaction_code", columnNames = "transaction_code")
        },
        indexes = {
                @Index(name = "idx_payments_booking_id", columnList = "booking_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_time", columnList = "payment_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod; // VNPAY, MOMO, CASH, BANKING

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    private String transactionCode;

    @Column(name = "payment_time", nullable = false)
    private LocalDateTime paymentTime;

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
        if (this.paymentTime == null) {
            this.paymentTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
