package com.football.data.discovery;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bộ lọc gắn dấu vết Instance (Instance Tracking Response Filter).
 * 
 * Tự động gắn 2 Header vào mọi HTTP Response trả về cho Client hoặc Gateway:
 * 1. 'X-Served-By-Instance': Định danh cụ thể instance nào đã phục vụ request (VD: booking-service:8081 [booking-service:8081:a1b2c3d4]).
 * 2. 'X-Service-Name': Tên định danh logic của Microservice.
 * 
 * Ý NGHĨA KỸ THUẬT CHO TH7:
 * Giúp Linh và Nhật kiểm chứng ngay lập tức thuật toán cân bằng tải Round-Robin
 * khi gọi liên tiếp API mà thấy giá trị Header này thay đổi luân phiên giữa các cổng (vd: 8081 -> 8083 -> 8081).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 7 (TH7)
 */
@Component
@RequiredArgsConstructor
public class InstanceTrackingFilter extends OncePerRequestFilter {

    private final InstanceMetadataProvider instanceMetadataProvider;

    public static final String HEADER_SERVED_BY = "X-Served-By-Instance";
    public static final String HEADER_SERVICE_NAME = "X-Service-Name";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Đóng dấu thông tin instance vào Response Header
        response.setHeader(HEADER_SERVED_BY, instanceMetadataProvider.getServingHeader());
        response.setHeader(HEADER_SERVICE_NAME, instanceMetadataProvider.getServiceName());

        filterChain.doFilter(request, response);
    }
}

