package com.pitchos.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitchos.booking.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 401 / 403 trả về cùng định dạng ErrorResponse với các lỗi khác (TH3 - response lỗi thống nhất). */
@Component
@RequiredArgsConstructor
public class JsonSecurityHandlers {

    static final String MISSING_TOKEN_MESSAGE = "Chưa đăng nhập hoặc thiếu token (Authorization: Bearer <token>)";
    static final String EXPIRED_TOKEN_MESSAGE = "Token đã hết hạn, vui lòng đăng nhập lại";
    static final String INVALID_TOKEN_MESSAGE = "Token không hợp lệ";
    public static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện thao tác này";

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> write(request, response, HttpStatus.UNAUTHORIZED, unauthorizedMessage(request));
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
    }

    private static String unauthorizedMessage(HttpServletRequest request) {
        Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (JwtAuthenticationFilter.ERROR_EXPIRED.equals(reason)) {
            return EXPIRED_TOKEN_MESSAGE;
        }
        if (JwtAuthenticationFilter.ERROR_INVALID.equals(reason)) {
            return INVALID_TOKEN_MESSAGE;
        }
        return MISSING_TOKEN_MESSAGE;
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status, message, request.getRequestURI(), null));
    }
}
