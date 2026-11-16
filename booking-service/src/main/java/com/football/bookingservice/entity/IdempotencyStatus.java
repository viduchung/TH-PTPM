package com.football.bookingservice.entity;

/** Trạng thái xử lý một Idempotency-Key (giá trị cột idempotency_keys.status). */
public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
