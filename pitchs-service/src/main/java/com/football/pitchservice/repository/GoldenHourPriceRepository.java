package com.football.pitchservice.repository;

import com.football.pitchservice.entity.GoldenHourPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoldenHourPriceRepository extends JpaRepository<GoldenHourPrice, Long> {
    List<GoldenHourPrice> findByPitchId(Long pitchId);
}

