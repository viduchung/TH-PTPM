package com.football.bookingservice.service;

import com.football.bookingservice.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * TH3 - Validation ngày giờ đặt sân (nghiệp vụ, ngoài các ràng buộc @NotNull của DTO).
 * Đơn phải nằm gọn trong MỘT ngày vì pitchs-service báo giá theo (ngày, giờ bắt đầu, giờ kết thúc).
 */
@Component
public class BookingTimeRules {

    public static final long MIN_DURATION_MINUTES = 30;
    public static final long MAX_DURATION_MINUTES = 4 * 60;
    public static final long MAX_DAYS_IN_ADVANCE = 60;

    private final Clock clock;

    public BookingTimeRules(Clock clock) {
        this.clock = clock;
    }

    public void validate(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("endTime phải sau startTime");
        }
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new BadRequestException("Giờ bắt đầu và kết thúc phải trong cùng một ngày");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!startTime.isAfter(now)) {
            throw new BadRequestException("startTime phải ở thời điểm trong tương lai");
        }
        if (startTime.isAfter(now.plusDays(MAX_DAYS_IN_ADVANCE))) {
            throw new BadRequestException("Chỉ được đặt trước tối đa " + MAX_DAYS_IN_ADVANCE + " ngày");
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
            throw new BadRequestException("Thời lượng đặt sân phải từ " + MIN_DURATION_MINUTES + " phút đến "
                    + MAX_DURATION_MINUTES / 60 + " giờ");
        }
    }

    public boolean hasStarted(LocalDateTime startTime) {
        return !startTime.isAfter(LocalDateTime.now(clock));
    }
}
