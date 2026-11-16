package com.football.bookingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bảng idempotency_keys (thiết kế của Hùng trong 03_booking_db_init.sql, TH6).
 * Khóa lưu dạng "{userId}:{Idempotency-Key}" để key của khách này không bao giờ trả về đơn của khách khác.
 */
@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key"))
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {

    public static final String RESOURCE_BOOKING_CREATION = "BOOKING_CREATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IdempotencyStatus status;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public IdempotencyRecord(String idempotencyKey, String resourceType) {
        this.idempotencyKey = idempotencyKey;
        this.resourceType = resourceType;
        this.status = IdempotencyStatus.PROCESSING;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
