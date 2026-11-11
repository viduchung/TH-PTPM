package com.football.authservice.repository;

import com.football.authservice.model.Role;
import com.football.authservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence Layer cho User (TH4) - Spring Data JPA.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
