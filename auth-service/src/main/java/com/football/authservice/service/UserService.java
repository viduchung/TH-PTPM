package com.football.authservice.service;

import com.football.authservice.dto.UserResponse;
import com.football.authservice.exception.ResourceNotFoundException;
import com.football.authservice.model.Role;
import com.football.authservice.model.User;
import com.football.authservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản trị người dùng (CRUD) - chỉ ADMIN (TH1 + TH5).
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String name, Role role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> result;
        if (role != null) {
            result = userRepository.findByRole(role, pageable);
        } else if (name != null && !name.isBlank()) {
            result = userRepository.findByUsernameContainingIgnoreCase(name, pageable);
        } else {
            result = userRepository.findAll(pageable);
        }
        return result.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + id));
    }

    /** Đổi vai trò người dùng (quan hệ vai trò - TH4/TH5). */
    @Transactional
    public UserResponse changeRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + id));
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    /** Khóa/mở khóa tài khoản. */
    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + id));
        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng id=" + id);
        }
        userRepository.deleteById(id);
    }
}
