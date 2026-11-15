package com.pitchos.booking.exception;

/** Đã đăng nhập nhưng không có quyền trên tài nguyên này (ví dụ xem đơn của người khác) → 403 Forbidden. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
