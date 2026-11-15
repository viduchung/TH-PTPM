package com.football.data.idempotency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ khóa bất biến (Idempotency Record).
 * Ánh xạ tới bảng `idempotency_keys` trong CSDL `booking_db`.
 * 
 * Mục tiêu: Ngăn chặn triệt để tình trạng tạo 2 đơn đặt sân hoặc trừ tiền 2 lần
 * khi client retry hoặc mạng chập chờn trong quá trình gọi tích hợp.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_idempotency_status", columnList = "idempotency_key, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

    @Column(name = "resource_id")
    private Long resourceId;

    @Lob
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

