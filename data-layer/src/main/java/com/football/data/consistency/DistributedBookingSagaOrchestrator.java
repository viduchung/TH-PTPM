package com.football.data.consistency;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.booking.repository.BookingRepository;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.exception.BadRequestException;
import com.football.data.exception.ConflictException;
import com.football.data.exception.ResourceNotFoundException;
import com.football.data.idempotency.service.IdempotencyService;
import com.football.data.mapper.BookingMapper;
import com.football.data.payment.entity.Payment;
import com.football.data.payment.repository.PaymentRepository;
import com.football.data.pitch.entity.Pitch;
import com.football.data.pitch.repository.PitchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Điều phối viên giao dịch phân tán (Distributed Saga Orchestrator).
 * 
 * Quản lý quy trình tích hợp liên dịch vụ 3 bước giữa booking-service, pitchs-service và payment-service:
 * 1. Kiểm tra sân khả dụng & không trùng lịch (pitchs-service / pitch_db).
 * 2. Tạo đơn đặt sân tạm thời ở trạng thái CHO_DUYET (booking-service / booking_db).
 * 3. Xử lý thanh toán cọc / toàn phần (payment-service / payment_db).
 * 4. Nếu thanh toán thành công: Chuyển trạng thái đơn sang DA_DUYET (Hoàn tất chu trình).
 * 5. Nếu thanh toán thất bại: Tự động kích hoạt GIAO DỊCH BÙ TRỪ (Compensating Transaction)
 *    chuyển trạng thái đơn sang TU_CHOI và giải phóng khung giờ thi đấu.
 * 6. Đóng gói trong IdempotencyService để bảo đảm tính bất biến khi client retry.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedBookingSagaOrchestrator {

    private final BookingRepository bookingRepository;
    private final PitchRepository pitchRepository;
    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final BookingStateTransitionValidator stateValidator;

    /**
     * Thực thi quy trình Saga với cơ chế Idempotency
     */
    public BookingResponseDTO executeBookingSaga(BookingRequestDTO request,
                                                 String paymentMethod,
                                                 String idempotencyKey) {
        return idempotencyService.executeIdempotent(
                idempotencyKey,
                "BOOKING_SAGA",
                BookingResponseDTO.class,
                () -> doBookingSaga(request, paymentMethod)
        );
    }

    /**
     * Thực thi nghiệp vụ Saga cốt lõi
     */
    private BookingResponseDTO doBookingSaga(BookingRequestDTO request, String paymentMethod) {
        log.info("[Saga] Bắt đầu điều phối đặt sân cho User #{} tại Pitch #{}", request.getUserId(), request.getPitchId());

        // Bước 1: Kiểm tra tính tồn tại của sân bóng
        Pitch pitch = pitchRepository.findById(request.getPitchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sân bóng", "id", request.getPitchId()));

        // Bước 2: Kiểm tra xung đột trùng lịch (Thuật toán toán học của Hùng)
        long overlappingCount = bookingRepository.countOverlappingBookings(
                request.getPitchId(), request.getStartTime(), request.getEndTime()
        );
        if (overlappingCount > 0) {
            log.warn("[Saga] Xung đột lịch thi đấu tại sân #{} từ {} đến {}",
                    request.getPitchId(), request.getStartTime(), request.getEndTime());
            throw new ConflictException("Khung giờ thi đấu này đã có đơn đặt trước. Vui lòng chọn giờ khác!");
        }

        // Tính toán tổng tiền
        double hours = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes() / 60.0;
        BigDecimal totalPrice = pitch.getPricePerHour().multiply(BigDecimal.valueOf(hours))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Bước 3: Tạo Booking ở trạng thái khởi tạo CHO_DUYET
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .pitchId(request.getPitchId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .pricePerHour(pitch.getPricePerHour())
                .totalPrice(totalPrice)
                .status(BookingStatus.CHO_DUYET)
                .note(request.getNote())
                .build();
        booking = bookingRepository.save(booking);
        log.info("[Saga] Đã tạo đơn đặt sân tạm thời #{} ở trạng thái CHO_DUYET", booking.getId());

        // Bước 4: Tích hợp gọi Payment
        boolean isPaymentSuccessful = paymentMethod != null && !paymentMethod.equalsIgnoreCase("FAIL");

        if (!isPaymentSuccessful) {
            log.warn("[Saga] Thanh toán thất bại cho đơn #{}. Kích hoạt giao dịch bù trừ (Compensating Transaction)...", booking.getId());

            // GIAO DỊCH BÙ TRỪ (Compensating Transaction)
            stateValidator.validateTransition(booking.getStatus(), BookingStatus.TU_CHOI);
            booking.setStatus(BookingStatus.TU_CHOI);
            bookingRepository.save(booking);

            throw new BadRequestException("Thanh toán không thành công. Giao dịch bù trừ đã tự động cập nhật đơn đặt sang trạng thái TU_CHOI.");
        }

        // Bước 5: Thanh toán thành công -> Ghi nhận payment_db và chuyển trạng thái DA_DUYET
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .amount(totalPrice)
                .paymentMethod(paymentMethod)
                .status("SUCCESS")
                .transactionCode("TXN_SAGA_" + System.currentTimeMillis() + "_" + booking.getId())
                .note("Thanh toán thành công trong chu trình Saga cho đơn #" + booking.getId())
                .build();
        paymentRepository.save(payment);

        stateValidator.validateTransition(booking.getStatus(), BookingStatus.DA_DUYET);
        booking.setStatus(BookingStatus.DA_DUYET);
        booking = bookingRepository.save(booking);

        log.info("[Saga] Hoàn tất chu trình Saga thành công cho đơn #{}. Trạng thái: DA_DUYET", booking.getId());
        return BookingMapper.toDTO(booking, pitch.getName());
    }
}
