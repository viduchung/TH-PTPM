package com.pitchos.booking.exception;

/**
 * Không xác minh được thông tin sân từ Pitch Service: tắt tiến trình, mất kết nối, quá thời gian chờ,
 * lỗi 5xx hoặc dữ liệu trả về không hợp lệ → 503 Service Unavailable (không tạo đơn, không làm sập Booking).
 */
public class PitchServiceUnavailableException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Dịch vụ quản lý sân hiện không khả dụng. Vui lòng thử lại sau!";

    public PitchServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public PitchServiceUnavailableException(String message) {
        super(message);
    }
}
