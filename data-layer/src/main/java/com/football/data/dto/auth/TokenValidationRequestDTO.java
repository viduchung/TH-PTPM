package com.football.data.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Yêu cầu kiểm tra tính hợp lệ của Token JWT khi các service gọi sang Auth Service
 * (Service-to-Service Token Validation Request - TH2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationRequestDTO {

    @NotBlank(message = "Token không được để trống")
    private String token;
}

