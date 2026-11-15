package com.football.data.auth.repository;

import com.football.data.auth.entity.Role;
import com.football.data.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu Người dùng - Spring Data JPA
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    List<User> findByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
