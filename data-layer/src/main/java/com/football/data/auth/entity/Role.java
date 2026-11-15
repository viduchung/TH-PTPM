package com.football.data.auth.entity;

/**
 * Vai trò người dùng trong hệ thống (RBAC)
 * Phụ trách miền dữ liệu: auth_db (Hoàng & Hùng)
 */
public enum Role {

    /** Khách hàng / Cầu thủ đặt sân */
    ROLE_CUSTOMER("Khách hàng"),

    /** Chủ sân bóng đá quản lý cụm sân */
    ROLE_FIELD_OWNER("Chủ sân bóng"),

    /** Quản trị viên toàn hệ thống */
    ROLE_ADMIN("Quản trị viên"),

    /** Dịch vụ nội bộ gọi liên service (Machine-to-Machine) */
    ROLE_INTERNAL_SERVICE("Dịch vụ nội bộ");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String authority() {
        return name().substring("ROLE_".length());
    }
}

