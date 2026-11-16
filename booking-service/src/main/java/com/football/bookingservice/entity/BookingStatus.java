package com.football.bookingservice.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Trạng thái đơn đặt sân - trùng giá trị cột bookings.status trong database/03_booking_db_init.sql.
 * Luồng chuyển trạng thái theo BookingStateTransitionValidator của data-layer (Hùng, TH6):
 *   CHO_DUYET -> DA_DUYET | TU_CHOI | DA_HUY
 *   DA_DUYET  -> DA_HUY
 *   TU_CHOI, DA_HUY: trạng thái kết thúc.
 */
public enum BookingStatus {

    CHO_DUYET("Chờ duyệt"),
    DA_DUYET("Đã duyệt"),
    TU_CHOI("Từ chối"),
    DA_HUY("Đã hủy");

    private final String description;

    BookingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** Đơn bị từ chối hoặc đã hủy thì nhả khung giờ cho người khác đặt. */
    public boolean occupiesPitchSlot() {
        return this != TU_CHOI && this != DA_HUY;
    }

    public boolean canTransitionTo(BookingStatus target) {
        return allowedTargets().contains(target);
    }

    private Set<BookingStatus> allowedTargets() {
        return switch (this) {
            case CHO_DUYET -> EnumSet.of(DA_DUYET, TU_CHOI, DA_HUY);
            case DA_DUYET -> EnumSet.of(DA_HUY);
            case TU_CHOI, DA_HUY -> Collections.emptySet();
        };
    }

    /** Các trạng thái không còn giữ khung giờ - dùng trong truy vấn kiểm tra trùng lịch. */
    public static Set<BookingStatus> releasedStatuses() {
        return EnumSet.of(TU_CHOI, DA_HUY);
    }
}
