package com.football.bookingservice.repository;

import com.football.bookingservice.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Giành lại quyền xử lý một key bằng MỘT câu UPDATE có điều kiện (nguyên tử, đúng cả khi nhiều instance):
     * key đã FAILED, hoặc PROCESSING quá lâu (instance trước bị tắt giữa chừng). Trả về số dòng được cập nhật.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IdempotencyRecord r
            SET r.status = com.football.bookingservice.entity.IdempotencyStatus.PROCESSING, r.updatedAt = :now
            WHERE r.idempotencyKey = :key
              AND (r.status = com.football.bookingservice.entity.IdempotencyStatus.FAILED
                   OR (r.status = com.football.bookingservice.entity.IdempotencyStatus.PROCESSING
                       AND r.updatedAt < :staleBefore))
            """)
    int takeOver(@Param("key") String key,
                 @Param("now") LocalDateTime now,
                 @Param("staleBefore") LocalDateTime staleBefore);
}
