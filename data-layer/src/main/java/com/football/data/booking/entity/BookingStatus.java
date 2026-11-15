package com.football.data.booking.entity;

/**
 * Trạng thái lượt đặt sân bóng
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 */
public enum BookingStatus {

    /** Đơn mới tạo, đang chờ chủ sân hoặc hệ thống duyệt */
    CHO_DUYET("Chờ duyệt"),

    /** Đơn đã được chấp thuận và giữ chỗ chắc chắn */
    DA_DUYET("Đã duyệt"),

    /** Đơn bị từ chối do sân bận hoặc lý do khách quan (không tính trùng lịch) */
    TU_CHOI("Từ chối"),

    /** Đơn do khách hàng tự hủy hoặc hủy trước giờ đá (không tính trùng lịch) */
    DA_HUY("Đã hủy");

    private final String description;

    BookingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Kiểm tra xem trạng thái này có còn chiếm dụng khung giờ của sân hay không.
     * Nếu là TU_CHOI hoặc DA_HUY thì khung giờ đã được giải phóng (nhả slot).
     */
    public boolean occupiesPitchSlot() {
        return this != TU_CHOI && this != DA_HUY;
    }
}
