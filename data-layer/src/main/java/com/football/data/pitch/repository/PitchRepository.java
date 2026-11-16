package com.football.data.pitch.repository;

import com.football.data.pitch.entity.Pitch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy xuất dữ liệu Sân bóng - Spring Data JPA
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 */
@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {

    List<Pitch> findByType(String type);

    List<Pitch> findByStatus(String status);

    List<Pitch> findByTypeAndStatus(String type, String status);

    List<Pitch> findByNameContainingIgnoreCase(String keyword);

    /**
     * Tìm kiếm và lọc sân bóng kết hợp phân trang
     */
    @Query("SELECT p FROM Pitch p WHERE " +
           "(:type IS NULL OR :type = '' OR p.type = :type) AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status)")
    Page<Pitch> searchPitches(@Param("type") String type,
                              @Param("status") String status,
                              Pageable pageable);
}

