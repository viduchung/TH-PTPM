package com.football.bookingservice.security;

/**
 * Danh tính lấy từ JWT, không truy vấn DB (stateless, TH5).
 *
 * @param username    claim sub
 * @param userId      claim userId nếu token có (chuẩn data-layer); token của auth-service hiện KHÔNG có claim này,
 *                    khi đó UserIdResolver hỏi auth-service qua /api/v1/auth/me
 * @param role        null nếu vai trò trong token không hợp lệ
 * @param bearerToken token gốc để chuyển tiếp sang auth-service (token propagation, TH6); không bao giờ ghi log
 */
public record AuthenticatedUser(String username, Long userId, AppRole role, String bearerToken) {

    public static final String INTERNAL_PRINCIPAL = "internal-service";

    public static AuthenticatedUser internalService() {
        return new AuthenticatedUser(INTERNAL_PRINCIPAL, null, AppRole.INTERNAL_SERVICE, null);
    }

    public boolean isCustomer() {
        return role == AppRole.CUSTOMER;
    }

    public boolean isOwner() {
        return role == AppRole.OWNER;
    }

    public boolean isAdmin() {
        return role == AppRole.ADMIN;
    }

    public boolean isInternalService() {
        return role == AppRole.INTERNAL_SERVICE;
    }

    /** Không để token lọt vào log khi in đối tượng. */
    @Override
    public String toString() {
        return "AuthenticatedUser[username=" + username + ", userId=" + userId + ", role=" + role + "]";
    }
}
