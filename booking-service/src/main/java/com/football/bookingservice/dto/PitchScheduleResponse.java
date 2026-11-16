package com.football.bookingservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lịch đã kín của một sân trong một ngày (TH1 - xem lịch đặt sân theo thời gian).
 * Công khai nên KHÔNG chứa userId, giá hay ghi chú của người đặt.
 */
public record PitchScheduleResponse(Long pitchId, LocalDate date, List<BusySlot> busySlots) {

    public record BusySlot(LocalDateTime startTime, LocalDateTime endTime, String status) {
    }
}
