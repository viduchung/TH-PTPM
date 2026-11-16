package com.football.authservice.dto;

/**
 * Phản hồi sau đăng nhập thành công (TH5) - chứa JWT Access Token.
 */
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String username;
    private String role;
    private String fullName;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, long expiresIn, String username, String role, String fullName) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
