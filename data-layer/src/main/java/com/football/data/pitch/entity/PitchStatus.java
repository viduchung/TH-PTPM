package com.football.data.pitch.entity;

/**
 * Trạng thái sân bóng đá
 */
public enum PitchStatus {
    AVAILABLE("Sẵn sàng cho thuê"),
    BOOKED("Đang có người đặt"),
    MAINTENANCE("Đang bảo trì");

    private final String description;

    PitchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
