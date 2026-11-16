package com.football.authservice.controller;

import com.football.authservice.dto.UserResponse;
import com.football.authservice.model.Role;
import com.football.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Quản trị người dùng - chỉ ROLE_ADMIN (TH1 CRUD + TH5 phân quyền).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Quản trị người dùng (chỉ ADMIN)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Danh sách người dùng (phân trang, lọc theo tên/vai trò)")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.list(name, role, page, size));
    }

    @Operation(summary = "Chi tiết người dùng theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Đổi vai trò người dùng")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        Role role = Role.valueOf(body.get("role"));
        return ResponseEntity.ok(userService.changeRole(id, role));
    }

    @Operation(summary = "Khóa / mở khóa tài khoản")
    @PatchMapping("/{id}/active")
    public ResponseEntity<UserResponse> setActive(@PathVariable Long id,
                                                  @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(userService.setActive(id, active));
    }

    @Operation(summary = "Xóa người dùng")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
