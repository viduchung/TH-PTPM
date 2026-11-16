package com.football.bookingservice.repository;

import com.football.bookingservice.entity.PitchLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PitchLockRepository extends JpaRepository<PitchLock, Long> {

    /** SELECT ... FOR UPDATE: transaction khác đặt cùng sân phải chờ tới khi transaction này commit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM PitchLock l WHERE l.pitchId = :pitchId")
    Optional<PitchLock> lockByPitchId(@Param("pitchId") Long pitchId);
}
