package com.football.authservice.dto;

/**
 * Kết quả xác thực token cho các service khác gọi sang (TH2 + TH6 token propagation).
 * Booking/Payment service gọi POST /api/v1/auth/validate để biết token hợp lệ không
 * và danh tính + vai trò của người dùng.
 */
public class TokenValidationResponse {

    private boolean valid;
    private String username;
    private String role;
    private String fullName;
    private String message;

    public static TokenValidationResponse valid(String username, String role, String fullName) {
        TokenValidationResponse r = new TokenValidationResponse();
        r.valid = true;
        r.username = username;
        r.role = role;
        r.fullName = fullName;
        r.message = "Token hợp lệ";
        return r;
    }

    public static TokenValidationResponse invalid(String message) {
        TokenValidationResponse r = new TokenValidationResponse();
        r.valid = false;
        r.message = message;
        return r;
    }

    public boolean isValid() { return valid; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getMessage() { return message; }
}
