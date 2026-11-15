package com.pitchos.booking.service;

import com.pitchos.booking.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/** TH3 - Validation ngày giờ đặt sân và cách tính tiền (giữ công thức cũ: giá × số phút / 60). */
@Component
@RequiredArgsConstructor
public class BookingTimeRules {

    public static final long MIN_DURATION_MINUTES = 30;
    public static final long MAX_DURATION_MINUTES = 8 * 60;
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);
    private static final int MONEY_SCALE = 2;

    private final Clock clock;

    /** Ném 400 nếu khung giờ không hợp lệ. Thứ tự kiểm tra giữ thông báo cũ "endTime phải sau startTime". */
    public void validate(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime phải sau startTime");
        }
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("startTime phải ở thời điểm trong tương lai");
        }
        long minutes = durationMinutes(startTime, endTime);
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
            throw new BadRequestException("Thời lượng đặt sân phải từ " + MIN_DURATION_MINUTES + " phút đến "
                    + (MAX_DURATION_MINUTES / 60) + " giờ");
        }
    }

    public boolean hasStarted(LocalDateTime startTime) {
        return !startTime.isAfter(LocalDateTime.now(clock));
    }

    public static long durationMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        return Duration.between(startTime, endTime).toMinutes();
    }

    public static BigDecimal totalPrice(BigDecimal pricePerHour, LocalDateTime startTime, LocalDateTime endTime) {
        return pricePerHour
                .multiply(BigDecimal.valueOf(durationMinutes(startTime, endTime)))
                .divide(MINUTES_PER_HOUR, MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
