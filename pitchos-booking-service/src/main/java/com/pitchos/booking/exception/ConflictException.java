package com.pitchos.booking.exception;

/** Xung đột trạng thái (Idempotency-Key dùng lại với nội dung khác, cập nhật đồng thời...) → 409 Conflict. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
