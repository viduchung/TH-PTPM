package com.pitchos.booking.service;

import com.pitchos.booking.dto.BookingResponse;
import com.pitchos.booking.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dữ liệu bất biến truyền giữa BookingService (điều phối) và BookingWriter (transaction). */
public final class BookingCommands {

    private BookingCommands() {
    }

    /** Khung giờ đã kiểm tra hợp lệ và giá đã tính từ dữ liệu Pitch Service. */
    public record PricedSlot(Long pitchId, LocalDateTime startTime, LocalDateTime endTime,
                             BigDecimal pricePerHour, BigDecimal totalPrice) {
    }

    public record NewBooking(Long userId, PricedSlot slot, String idempotencyKey) {
    }

    /** Kết quả tạo đơn: replayed = true khi request gửi lại với cùng Idempotency-Key (không tạo đơn mới). */
    public record CreateOutcome(BookingResponse booking, boolean replayed) {

        static CreateOutcome created(Booking booking) {
            return new CreateOutcome(BookingResponse.from(booking), false);
        }

        static CreateOutcome replayed(Booking booking) {
            return new CreateOutcome(BookingResponse.from(booking), true);
        }
    }
}
