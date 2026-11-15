package com.football.data.idempotency.repository;

import com.football.data.idempotency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản trị dữ liệu khóa bất biến `idempotency_keys`.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Tìm kiếm bản ghi Idempotency theo khóa duy nhất
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Kiểm tra sự tồn tại của khóa
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
