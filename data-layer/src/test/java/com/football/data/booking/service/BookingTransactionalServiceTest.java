package com.football.data.booking.service;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.booking.repository.BookingRepository;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.exception.BadRequestException;
import com.football.data.exception.ConflictException;
import com.football.data.payment.entity.Payment;
import com.football.data.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class BookingTransactionalServiceTest {

    @Autowired
    private BookingTransactionalService transactionalService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    @DisplayName("TH4 Transactional 1: Tạo đơn thành công được COMMIT bền vững xuống CSDL")
    void testCreateBooking_CommitSuccess() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(1L)
                .pitchId(10L)
                .startTime(LocalDateTime.of(2026, 11, 25, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 25, 19, 30))
                .note("Giao lưu thể thao")
                .build();

        BookingResponseDTO response = transactionalService.createBooking(request, new BigDecimal("300000.00"));

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTotalPrice()).isEqualByComparingTo("450000.00");

        // Kiểm chứng dữ liệu đã được ghi thật vào Repository
        List<Booking> list = bookingRepository.findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo(BookingStatus.CHO_DUYET);
    }

    @Test
    @DisplayName("TH4 Transactional 2: Tự động ROLLBACK giao dịch khi bị trùng lịch thi đấu")
    void testCreateBooking_RollbackOnConflict() {
        // Đặt trước 1 đơn hợp lệ: 18:00 - 19:30
        Booking initialBooking = Booking.builder()
                .userId(1L)
                .pitchId(10L)
                .startTime(LocalDateTime.of(2026, 11, 25, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 25, 19, 30))
                .totalPrice(new BigDecimal("450000.00"))
                .status(BookingStatus.DA_DUYET)
                .build();
        bookingRepository.save(initialBooking);
        assertThat(bookingRepository.count()).isEqualTo(1L);

        // Khách thứ 2 cố tình đặt trùng khung giờ: 18:30 - 20:00
        BookingRequestDTO conflictRequest = BookingRequestDTO.builder()
                .userId(2L)
                .pitchId(10L)
                .startTime(LocalDateTime.of(2026, 11, 25, 18, 30))
                .endTime(LocalDateTime.of(2026, 11, 25, 20, 0))
                .build();

        assertThatThrownBy(() -> transactionalService.createBooking(conflictRequest, new BigDecimal("300000.00")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Khung giờ này sân bóng đã có người đặt trước");

        // CHỨNG MINH TÍNH TOÀN VẸN VÀ ROLLBACK: Số lượng bản ghi trong DB vẫn giữ nguyên là 1
        assertThat(bookingRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("TH4 Transactional 3: Giao dịch nguyên tử Thanh toán + Duyệt đơn cùng COMMIT thành công")
    void testProcessPaymentAndConfirmBooking_Success() {
        Booking booking = Booking.builder()
                .userId(1L)
                .pitchId(10L)
                .startTime(LocalDateTime.of(2026, 11, 25, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 25, 19, 30))
                .totalPrice(new BigDecimal("450000.00"))
                .status(BookingStatus.CHO_DUYET)
                .build();
        booking = bookingRepository.save(booking);

        Payment payment = transactionalService.processPaymentAndConfirmBooking(
                booking.getId(), new BigDecimal("450000.00"), "VNPAY");

        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getTransactionCode()).startsWith("TXN_");

        // Kiểm tra booking đã chuyển sang DA_DUYET
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.DA_DUYET);
    }

    @Test
    @DisplayName("TH4 Transactional 4: Tự động ROLLBACK toàn bộ khi số tiền thanh toán không đủ")
    void testProcessPayment_RollbackOnUnderpayment() {
        Booking booking = Booking.builder()
                .userId(1L)
                .pitchId(10L)
                .startTime(LocalDateTime.of(2026, 11, 25, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 25, 19, 30))
                .totalPrice(new BigDecimal("500000.00"))
                .status(BookingStatus.CHO_DUYET)
                .build();
        booking = bookingRepository.save(booking);

        // Khách chỉ thanh toán 200.000đ trong khi tổng tiền là 500.000đ -> ném lỗi và ROLLBACK
        Long bookingId = booking.getId();
        assertThatThrownBy(() -> transactionalService.processPaymentAndConfirmBooking(
                bookingId, new BigDecimal("200000.00"), "MOMO"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số tiền thanh toán không được nhỏ hơn");

        // CHỨNG MINH ROLLBACK: Trạng thái đơn vẫn là CHO_DUYET, không tạo bản ghi thanh toán
        Booking afterRollback = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(afterRollback.getStatus()).isEqualTo(BookingStatus.CHO_DUYET);
        assertThat(paymentRepository.count()).isEqualTo(0L);
    }
}

