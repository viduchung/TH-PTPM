package com.pitchos.booking.entity;

import java.util.Set;

/**
 * Trạng thái đơn đặt sân (giữ nguyên 3 giá trị cũ, thêm da_huy cho nghiệp vụ hủy đơn TH1).
 *   cho_duyet → da_duyet | tu_choi (chủ sân / admin)   · cho_duyet | da_duyet → da_huy (khách / admin)
 * Đơn tu_choi và da_huy là trạng thái cuối: đã nhả khung giờ nên không được đổi nữa.
 */
public enum BookingStatus {
    cho_duyet,
    da_duyet,
    tu_choi,
    da_huy;

    /** Các trạng thái KHÔNG còn giữ khung giờ, bỏ qua khi kiểm tra trùng lịch. */
    public static final Set<BookingStatus> RELEASED_SLOT = Set.of(tu_choi, da_huy);

    public boolean isFinal() {
        return RELEASED_SLOT.contains(this);
    }
}
