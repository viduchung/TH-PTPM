package com.football.authservice.model;

/**
 * Vai trò người dùng (RBAC) - TH5.
 * 3 Actor cốt lõi trong đề tài Quản lý sân bóng đá.
 * Lưu trong CSDL dưới dạng chuỗi (EnumType.STRING).
 */
public enum Role {

    /** Người thuê sân / cầu thủ: đặt & quản lý đơn của chính mình. */
    ROLE_CUSTOMER("Khách hàng"),

    /** Chủ sân / quản lý sân: duyệt đơn, xem thống kê doanh thu. */
    ROLE_FIELD_OWNER("Chủ sân bóng"),

    /** Quản trị viên toàn hệ thống: quyền hạn tối cao. */
    ROLE_ADMIN("Quản trị viên"),

    /** Vai trò nội bộ dành cho gọi liên dịch vụ (Machine-to-Machine) - TH6. */
    ROLE_INTERNAL_SERVICE("Dịch vụ nội bộ");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Tên quyền dùng cho Spring Security hasRole(...) - bỏ tiền tố ROLE_. */
    public String authority() {
        return name().substring("ROLE_".length());
    }
}
