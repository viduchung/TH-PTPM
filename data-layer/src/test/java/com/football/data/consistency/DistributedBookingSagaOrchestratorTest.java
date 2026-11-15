package com.football.data.consistency;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.booking.repository.BookingRepository;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.exception.BadRequestException;
import com.football.data.exception.ConflictException;
import com.football.data.idempotency.repository.IdempotencyRecordRepository;
import com.football.data.payment.entity.Payment;
import com.football.data.payment.repository.PaymentRepository;
import com.football.data.pitch.entity.Pitch;
import com.football.data.pitch.entity.PitchStatus;
import com.football.data.pitch.entity.PitchType;
import com.football.data.pitch.repository.PitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TH6 Integration Test: Điều phối Saga & Giao dịch bù trừ (DistributedBookingSagaOrchestrator)")
public class DistributedBookingSagaOrchestratorTest {

    @Autowired
    private DistributedBookingSagaOrchestrator sagaOrchestrator;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private Pitch testPitch;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        paymentRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        pitchRepository.deleteAll();

        testPitch = Pitch.builder()
                .name("Sân 7 SAGA Test")
                .type("SAN_7")
                .status("AVAILABLE")
                .pricePerHour(new BigDecimal("300000.00"))
                .address("Trung tâm TDTT Cầu Giấy")
                .build();
        testPitch = pitchRepository.save(testPitch);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        paymentRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        pitchRepository.deleteAll();
    }

    @Test
    @DisplayName("TH6 Test 1: Chu trình Saga thành công trọn vẹn (Pitch -> Booking -> Payment -> DA_DUYET)")
    void testSaga_HappyPath_Success() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(10L)
                .pitchId(testPitch.getId())
                .startTime(LocalDateTime.of(2026, 12, 10, 18, 0))
                .endTime(LocalDateTime.of(2026, 12, 10, 20, 0)) // 2 giờ
                .note("Đá giải bóng đá sinh viên")
                .build();

        String idempotencyKey = "saga-key-001";

        BookingResponseDTO response = sagaOrchestrator.executeBookingSaga(request, "VNPAY", idempotencyKey);

        assertNotNull(response);
        assertEquals(BookingStatus.DA_DUYET.name(), response.getStatus());
        assertEquals(2.0, response.getDurationHours());
        assertEquals(0, response.getTotalPrice().compareTo(new BigDecimal("600000.00")));

        // Kiểm tra CSDL booking_db
        Booking savedBooking = bookingRepository.findById(response.getId()).orElse(null);
        assertNotNull(savedBooking);
        assertEquals(BookingStatus.DA_DUYET, savedBooking.getStatus());

        // Kiểm tra CSDL payment_db
        List<Payment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        assertEquals(savedBooking.getId(), payments.get(0).getBookingId());
        assertEquals("SUCCESS", payments.get(0).getStatus());
        assertEquals(0, payments.get(0).getAmount().compareTo(new BigDecimal("600000.00")));
    }

    @Test
    @DisplayName("TH6 Test 2: Thanh toán thất bại -> Tự động kích hoạt Giao dịch bù trừ (Compensating Transaction) về TU_CHOI")
    void testSaga_PaymentFailure_CompensatesBookingToTuChoi() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(11L)
                .pitchId(testPitch.getId())
                .startTime(LocalDateTime.of(2026, 12, 11, 18, 0))
                .endTime(LocalDateTime.of(2026, 12, 11, 19, 30))
                .note("Giả lập lỗi thanh toán")
                .build();

        String idempotencyKey = "saga-key-fail-002";

        // Gửi phương thức FAIL để kích hoạt kịch bản thanh toán lỗi
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                sagaOrchestrator.executeBookingSaga(request, "FAIL", idempotencyKey)
        );

        assertTrue(exception.getMessage().contains("Giao dịch bù trừ đã tự động cập nhật"));

        // Kiểm chứng tính nhất quán dữ liệu: Đơn đặt sân đã tạo được bù trừ sang TU_CHOI chứ KHÔNG bị treo CHO_DUYET
        List<Booking> bookings = bookingRepository.findAll();
        assertEquals(1, bookings.size());
        assertEquals(BookingStatus.TU_CHOI, bookings.get(0).getStatus());

        // Không có giao dịch thanh toán thành công nào trong payment_db
        assertEquals(0, paymentRepository.count());
    }

    @Test
    @DisplayName("TH6 Test 3: Tính bất biến Saga: Gửi 2 lần cùng Idempotency Key -> Chỉ tạo 1 đơn đặt & 1 thanh toán")
    void testSaga_IdempotentReplay_DoesNotDuplicateData() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .userId(12L)
                .pitchId(testPitch.getId())
                .startTime(LocalDateTime.of(2026, 12, 12, 18, 0))
                .endTime(LocalDateTime.of(2026, 12, 12, 19, 30))
                .note("Kiểm thử tính bất biến")
                .build();

        String key = "saga-idempotent-key-003";

        // Lần gọi 1
        BookingResponseDTO res1 = sagaOrchestrator.executeBookingSaga(request, "BANKING", key);
        // Lần gọi 2 (Retry do mạng chập chờn)
        BookingResponseDTO res2 = sagaOrchestrator.executeBookingSaga(request, "BANKING", key);

        assertEquals(res1.getId(), res2.getId());
        assertEquals(res1.getTotalPrice(), res2.getTotalPrice());

        // Kiểm tra CSDL chỉ có duy nhất 1 booking và 1 payment
        assertEquals(1, bookingRepository.count(), "Chỉ được tồn tại duy nhất 1 đơn đặt sân");
        assertEquals(1, paymentRepository.count(), "Chỉ được tồn tại duy nhất 1 giao dịch thanh toán");
    }

    @Test
    @DisplayName("TH6 Test 4: Xung đột trùng lịch thi đấu -> Ném ConflictException, không tạo booking mồ côi")
    void testSaga_ScheduleConflict_ThrowsConflict() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 15, 20, 0);

        // Tạo sẵn một đơn đặt sân hợp lệ đã duyệt
        Booking existing = Booking.builder()
                .userId(1L)
                .pitchId(testPitch.getId())
                .startTime(start)
                .endTime(end)
                .pricePerHour(testPitch.getPricePerHour())
                .totalPrice(new BigDecimal("600000.00"))
                .status(BookingStatus.DA_DUYET)
                .build();
        bookingRepository.save(existing);

        // Đơn thứ 2 cố tình đặt trùng khung giờ
        BookingRequestDTO conflictRequest = BookingRequestDTO.builder()
                .userId(2L)
                .pitchId(testPitch.getId())
                .startTime(start.plusMinutes(30))
                .endTime(end.plusMinutes(30))
                .build();

        assertThrows(ConflictException.class, () ->
                sagaOrchestrator.executeBookingSaga(conflictRequest, "VNPAY", "saga-conflict-key")
        );

        // Số lượng booking vẫn giữ nguyên là 1
        assertEquals(1, bookingRepository.count());
    }
}
