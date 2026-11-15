package com.football.data.booking.service;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.booking.repository.BookingRepository;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.exception.BadRequestException;
import com.football.data.exception.ConflictException;
import com.football.data.exception.ResourceNotFoundException;
import com.football.data.mapper.BookingMapper;
import com.football.data.payment.entity.Payment;
import com.football.data.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service Quản lý Giao dịch CSDL (@Transactional) - TH4
 * 
 * =========================================================================
 * VAI TRÒ CỦA HÙNG TRONG BUỔI TH4:
 * 1. Thiết lập ranh giới giao dịch ACID an toàn cho tầng dữ liệu.
 * 2. Đảm bảo tính nguyên tử (Atomicity): Nếu xảy ra xung đột trùng lịch
 *    hoặc lỗi ngoại lệ, giao dịch được tự động ROLLBACK hoàn toàn.
 * 3. Ngăn ngừa Dirty Read và Lost Update bằng mức cô lập READ_COMMITTED.
 * 4. Tối ưu hóa truy vấn với @Transactional(readOnly = true).
 * =========================================================================
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 4 (TH4)
 */
@Service
@RequiredArgsConstructor
public class BookingTransactionalService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Tạo đơn đặt sân với ranh giới giao dịch ACID nghiêm ngặt.
     * Tự động Rollback nếu xảy ra trùng lịch hoặc lỗi dữ liệu.
     */
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public BookingResponseDTO createBooking(BookingRequestDTO request, BigDecimal pricePerHour) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Thời gian bắt đầu nhận sân phải trước thời gian trả sân");
        }

        // 1. Kiểm tra chống trùng lịch thi đấu bằng JPQL tối ưu (Hùng phụ trách)
        Long overlappingCount = bookingRepository.countOverlappingBookings(
                request.getPitchId(), request.getStartTime(), request.getEndTime());

        if (overlappingCount > 0) {
            // Ném ngoại lệ kích hoạt tự động ROLLBACK giao dịch
            throw new ConflictException("Khung giờ này sân bóng đã có người đặt trước. Giao dịch bị hủy bỏ!");
        }

        // 2. Tính toán tổng tiền
        java.time.Duration duration = java.time.Duration.between(request.getStartTime(), request.getEndTime());
        double hours = (double) duration.toMinutes() / 60.0;
        BigDecimal totalPrice = pricePerHour.multiply(BigDecimal.valueOf(hours));

        // 3. Ghi đĩa CSDL
        Booking newBooking = BookingMapper.toEntity(request, pricePerHour, totalPrice);
        Booking saved = bookingRepository.save(newBooking);

        return BookingMapper.toDTO(saved);
    }

    /**
     * Giao dịch nguyên tử: Duyệt đơn đặt sân VÀ Khởi tạo bản ghi thanh toán.
     * Cả hai thao tác phải cùng thành công (COMMIT) hoặc cùng thất bại (ROLLBACK).
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment processPaymentAndConfirmBooking(Long bookingId, BigDecimal amount, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt sân", "id", bookingId));

        if (booking.getStatus() == BookingStatus.TU_CHOI || booking.getStatus() == BookingStatus.DA_HUY) {
            throw new BadRequestException("Không thể thanh toán cho đơn đặt sân đã bị hủy hoặc từ chối");
        }

        if (amount.compareTo(booking.getTotalPrice()) < 0) {
            throw new BadRequestException("Số tiền thanh toán không được nhỏ hơn tổng tiền thuê sân: " + booking.getTotalPrice());
        }

        // 1. Cập nhật trạng thái đơn thành DA_DUYET
        booking.setStatus(BookingStatus.DA_DUYET);
        bookingRepository.save(booking);

        // 2. Tạo bản ghi giao dịch thanh toán
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status("SUCCESS")
                .transactionCode("TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .paymentTime(LocalDateTime.now())
                .note("Thanh toán thành công qua " + paymentMethod)
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Hủy đơn đặt sân an toàn: Giải phóng slot thi đấu cho các đội bóng khác.
     */
    @Transactional(rollbackFor = Exception.class)
    public BookingResponseDTO cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt sân", "id", bookingId));

        if (booking.getStatus() == BookingStatus.DA_HUY) {
            throw new BadRequestException("Đơn đặt sân này đã được hủy trước đó");
        }

        booking.setStatus(BookingStatus.DA_HUY);
        booking.setNote((booking.getNote() != null ? booking.getNote() + " | " : "") + "Lý do hủy: " + reason);
        Booking updated = bookingRepository.save(booking);

        return BookingMapper.toDTO(updated);
    }

    /**
     * Truy vấn an toàn ở chế độ Read-Only, giải phóng khóa và tối ưu bộ nhớ Hibernate session.
     */
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt sân", "id", id));
        return BookingMapper.toDTO(booking);
    }
}
