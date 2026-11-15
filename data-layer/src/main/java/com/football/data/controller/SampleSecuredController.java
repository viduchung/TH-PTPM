package com.football.data.controller;

import com.football.data.dto.common.ApiResponse;
import com.football.data.security.SecurityUtils;
import com.football.data.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller minh họa và kiểm thử toàn diện cơ chế bảo mật Common Security (TH5).
 * Kiểm chứng phân quyền RBAC và trích xuất thông tin người dùng từ SecurityContext.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 5 (TH5)
 */
@RestController
@RequestMapping("/api/secured")
public class SampleSecuredController {

    /**
     * Endpoint yêu cầu quyền CUSTOMER
     */
    @GetMapping("/customer/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerProfile() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", SecurityUtils.getCurrentUserId().orElse(null));
        data.put("username", SecurityUtils.getCurrentUsername().orElse(null));
        data.put("accessScope", "CUSTOMER_DATA");

        return ResponseEntity.ok(ApiResponse.success(data, "Truy cập thông tin khách hàng thành công"));
    }

    /**
     * Endpoint yêu cầu quyền FIELD_OWNER (hoặc ADMIN)
     */
    @GetMapping("/field-owner/pitches")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOwnerPitches() {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerId", SecurityUtils.getCurrentUserId().orElse(null));
        data.put("accessScope", "FIELD_OWNER_MANAGEMENT");

        return ResponseEntity.ok(ApiResponse.success(data, "Truy cập danh sách quản lý sân bóng"));
    }

    /**
     * Endpoint yêu cầu quyền ADMIN
     */
    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminUsers() {
        Map<String, Object> data = new HashMap<>();
        data.put("adminId", SecurityUtils.getCurrentUserId().orElse(null));
        data.put("accessScope", "SYSTEM_ADMINISTRATION");

        return ResponseEntity.ok(ApiResponse.success(data, "Truy cập tài nguyên quản trị hệ thống"));
    }

    /**
     * Endpoint kiểm chứng phân quyền bằng annotation @PreAuthorize
     */
    @GetMapping("/method-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> checkMethodAdmin() {
        return ResponseEntity.ok(ApiResponse.success("ADMIN_ACCESS_GRANTED", "Xác thực quyền hạn Method Security thành công"));
    }

    /**
     * Endpoint lấy thông tin người dùng hiện tại thông qua SecurityUtils
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin xác thực"));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", principal.getId());
        userInfo.put("username", principal.getUsername());
        userInfo.put("roles", principal.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList());

        return ResponseEntity.ok(ApiResponse.success(userInfo, "Lấy thông tin tài khoản thành công"));
    }
}
