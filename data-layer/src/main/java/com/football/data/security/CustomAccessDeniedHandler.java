package com.football.data.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.data.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý khi người dùng đã xác thực nhưng không đủ quyền hạn (Role/Permission)
 * để truy cập tài nguyên được bảo vệ (ví dụ: CUSTOMER truy cập tài nguyên ADMIN).
 * 
 * Trả về mã lỗi 403 Forbidden kèm cấu trúc JSON chuẩn RFC 7807 (ErrorResponse).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Truy cập bị từ chối do không đủ quyền hạn tại [{}]: {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Bạn không có quyền hạn thực hiện hành động này (Quyền truy cập bị từ chối)",
                request.getRequestURI(),
                "ERR_FORBIDDEN"
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

