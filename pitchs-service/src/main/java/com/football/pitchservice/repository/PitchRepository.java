package com.football.pitchservice.repository;

import com.football.pitchservice.entity.Pitch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long> {
    List<Pitch> findByType(String type);
    List<Pitch> findByStatus(String status);
}

