package com.football.data.concurrency;

import com.football.data.booking.entity.Booking;
import com.football.data.booking.entity.BookingStatus;
import com.football.data.booking.repository.BookingRepository;
import com.football.data.consistency.DistributedBookingSagaOrchestrator;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.idempotency.repository.IdempotencyRecordRepository;
import com.football.data.payment.repository.PaymentRepository;
import com.football.data.pitch.entity.Pitch;
import com.football.data.pitch.repository.PitchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TH7 Concurrency Test: Giả lập đa Instance tranh chấp đặt sân đồng thời (Race Condition Elimination)")
public class MultiInstanceConcurrencyRaceConditionTest {

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
                .name("Sân 7 Multi-Instance Test")
                .type("SAN_7")
                .status("AVAILABLE")
                .pricePerHour(new BigDecimal("350000.00"))
                .address("Sân vận động Mỹ Đình")
                .build();
        testPitch = pitchRepository.save(testPitch);
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        paymentRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        pitchRepository.deleteAll();
    }

    @Test
    @DisplayName("TH7 Test: 10 Instance/Luồng đồng thời tranh chấp đặt cùng 1 sân ở cùng 1 giờ -> Duy nhất 1 đơn thành công")
    void testConcurrentBookingRaceCondition_OnlyOneSucceeds() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        LocalDateTime startTime = LocalDateTime.of(2026, 12, 25, 19, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 12, 25, 20, 30); // 1.5 giờ

        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            final String idempotencyKey = "multi-instance-request-user-" + i;

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // Chờ cổng mở để tất cả 10 luồng cùng lao vào tại 1 mili-giây
                    startGate.await();

                    BookingRequestDTO request = BookingRequestDTO.builder()
                            .userId(userId)
                            .pitchId(testPitch.getId())
                            .startTime(startTime)
                            .endTime(endTime)
                            .note("Request đồng thời từ instance user #" + userId)
                            .build();

                    sagaOrchestrator.executeBookingSaga(request, "VNPAY", idempotencyKey);
                    successCount.incrementAndGet();

                } catch (com.football.data.exception.ConflictException ce) {
                    // Bị từ chối vì trùng lịch hoặc đang khóa xử lý
                    conflictCount.incrementAndGet();
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Chờ tất cả 10 luồng sẵn sàng
        readyLatch.await(5, TimeUnit.SECONDS);
        // Mở cổng bắn đồng thời
        startGate.countDown();
        // Chờ tất cả thực thi xong
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Tất cả các luồng phải hoàn tất trong 10 giây");

        // KIỂM CHỨNG KẾT QUẢ ĐẢM BẢO NHẤT QUÁN TOÀN VẸN
        assertEquals(1, successCount.get(), "Chỉ có duy nhất 1 đơn đặt sân thành công");
        assertEquals(9, conflictCount.get(), "9 request còn lại phải bị từ chối bằng ConflictException");
        assertTrue(exceptions.isEmpty(), "Không được có bất kỳ ngoại lệ hệ thống không kiểm soát nào: " + exceptions);

        // Kiểm tra CSDL chỉ có duy nhất 1 bản ghi Booking
        List<Booking> bookings = bookingRepository.findAll();
        assertEquals(1, bookings.size());
        assertEquals(BookingStatus.DA_DUYET, bookings.get(0).getStatus());

        // Kiểm tra CSDL payment chỉ có duy nhất 1 bản ghi Payment
        assertEquals(1, paymentRepository.count());
    }
}
