package com.football.data.payment;

import com.football.data.payment.entity.Payment;
import com.football.data.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("TH4 Payment: Lưu và truy xuất giao dịch thanh toán theo bookingId và transactionCode")
    void testSaveAndFindPayment() {
        Payment payment = Payment.builder()
                .bookingId(100L)
                .amount(new BigDecimal("450000.00"))
                .paymentMethod("VNPAY")
                .status("SUCCESS")
                .transactionCode("TXN_TEST_123456")
                .paymentTime(LocalDateTime.now())
                .note("Thanh toan san 7")
                .build();

        Payment saved = paymentRepository.save(payment);
        assertThat(saved.getId()).isNotNull();

        Optional<Payment> foundByCode = paymentRepository.findByTransactionCode("TXN_TEST_123456");
        assertThat(foundByCode).isPresent();
        assertThat(foundByCode.get().getAmount()).isEqualByComparingTo("450000.00");
        assertThat(foundByCode.get().getPaymentMethod()).isEqualTo("VNPAY");

        List<Payment> byBooking = paymentRepository.findByBookingId(100L);
        assertThat(byBooking).hasSize(1);
    }
}
