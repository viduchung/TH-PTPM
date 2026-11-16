package com.football.data.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.data.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý khi người dùng chưa xác thực hoặc gửi token JWT không hợp lệ
 * khi truy cập vào tài nguyên yêu cầu bảo mật.
 * 
 * Trả về mã lỗi 401 Unauthorized kèm cấu trúc JSON chuẩn RFC 7807 (ErrorResponse).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Truy cập trái phép vào endpoint [{}]: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Yêu cầu xác thực không hợp lệ hoặc thiếu token Bearer hợp lệ",
                request.getRequestURI(),
                "ERR_UNAUTHORIZED"
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

