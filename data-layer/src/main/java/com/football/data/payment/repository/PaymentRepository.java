package com.football.data.payment.repository;

import com.football.data.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu Thanh toán - Spring Data JPA
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 4 (TH4)
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByStatus(String status);
}
