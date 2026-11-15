package com.football.data.pitch.repository;

import com.football.data.pitch.entity.GoldenHourPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy xuất giá giờ vàng - Spring Data JPA
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 */
@Repository
public interface GoldenHourPriceRepository extends JpaRepository<GoldenHourPrice, Long> {

    List<GoldenHourPrice> findByPitchId(Long pitchId);
}
