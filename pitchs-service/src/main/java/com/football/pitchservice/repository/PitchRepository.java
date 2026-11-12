package com.football.pitchservice.repository;

import com.football.pitchservice.entity.Pitch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {

    List<Pitch> findByType(String type);
    List<Pitch> findByStatus(String status);

    // TH3: Tìm kiếm lọc theo loại sân và trạng thái kết hợp phân trang Pageable
    @Query("SELECT p FROM Pitch p WHERE " +
           "(:type IS NULL OR :type = '' OR p.type = :type) AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status)")
    Page<Pitch> searchPitches(@Param("type") String type,
                              @Param("status") String status,
                              Pageable pageable);
}
