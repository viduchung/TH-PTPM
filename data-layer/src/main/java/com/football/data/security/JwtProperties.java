package com.football.data.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tham số JSON Web Token (JWT) cho toàn bộ hệ thống.
 * Có thể ghi đè qua tệp application.yml hoặc biến môi trường.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Configuration
@ConfigurationProperties(prefix = "application.security.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Khóa bí mật ký số HMAC-SHA256 (tối thiểu 256 bits - 32 bytes).
     * Mặc định là chuỗi an toàn 256-bit chuẩn hóa cho môi trường phát triển & kiểm thử.
     */
    private String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /** Thời gian sống của Access Token (ms) - Mặc định 24 giờ */
    private long expirationMs = 86400000L;

    /** Thời gian sống của Refresh Token (ms) - Mặc định 7 ngày */
    private long refreshExpirationMs = 604800000L;

    /** Tên đơn vị phát hành token (Issuer) */
    private String issuer = "football-pitch-platform";

    /** Tiền tố Authorization Header */
    private String tokenPrefix = "Bearer ";

    /** Tên Header chứa JWT */
    private String headerString = "Authorization";
}

