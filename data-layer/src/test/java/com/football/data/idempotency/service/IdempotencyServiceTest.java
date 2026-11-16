package com.football.data.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.data.exception.ConflictException;
import com.football.data.idempotency.entity.IdempotencyRecord;
import com.football.data.idempotency.entity.IdempotencyStatus;
import com.football.data.idempotency.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TH6 Unit Test: Dịch vụ đảm bảo tính bất biến (IdempotencyService)")
public class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        idempotencyRecordRepository.deleteAll();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        idempotencyRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("TH6 Test 1: Thực thi lần đầu thành công, lưu bản ghi COMPLETED vào CSDL")
    void testExecuteIdempotent_FirstCall_Success() {
        String key = "test-key-001";
        AtomicInteger counter = new AtomicInteger(0);

        String result = idempotencyService.executeIdempotent(
                key, "TEST_RESOURCE", String.class, () -> {
                    counter.incrementAndGet();
                    return "Result from execution";
                }
        );

        assertEquals("Result from execution", result);
        assertEquals(1, counter.get());

        IdempotencyRecord record = idempotencyRecordRepository.findByIdempotencyKey(key).orElse(null);
        assertNotNull(record);
        assertEquals(IdempotencyStatus.COMPLETED, record.getStatus());
        assertTrue(record.getResponseBody().contains("Result from execution"));
    }

    @Test
    @DisplayName("TH6 Test 2: Gọi lặp lại cùng Idempotency Key -> Trả về cache, KHÔNG thực thi lại nghiệp vụ")
    void testExecuteIdempotent_RepeatCall_ReturnsCachedResponse() {
        String key = "test-key-002";
        AtomicInteger counter = new AtomicInteger(0);

        // Lần gọi 1
        String firstResult = idempotencyService.executeIdempotent(
                key, "TEST_RESOURCE", String.class, () -> {
                    counter.incrementAndGet();
                    return "Executed Value #" + counter.get();
                }
        );

        // Lần gọi 2 (Cùng key - giả lập client gửi lại request do mạng trễ)
        String secondResult = idempotencyService.executeIdempotent(
                key, "TEST_RESOURCE", String.class, () -> {
                    counter.incrementAndGet();
                    return "Executed Value #" + counter.get();
                }
        );

        assertEquals("Executed Value #1", firstResult);
        assertEquals("Executed Value #1", secondResult);
        assertEquals(1, counter.get(), "Hàm nghiệp vụ chỉ được chạy duy nhất 1 lần!");
    }

    @Test
    @DisplayName("TH6 Test 3: Request đồng thời có cùng Idempotency Key đang ở PROCESSING -> Ném ra ConflictException")
    void testExecuteIdempotent_ConcurrentProcessing_ThrowsConflictException() {
        String key = "test-key-003";

        // Tạo sẵn một bản ghi ở trạng thái PROCESSING
        IdempotencyRecord inProgressRecord = IdempotencyRecord.builder()
                .idempotencyKey(key)
                .resourceType("TEST_RESOURCE")
                .status(IdempotencyStatus.PROCESSING)
                .build();
        idempotencyRecordRepository.save(inProgressRecord);

        // Thử thực thi với key đang bận
        ConflictException exception = assertThrows(ConflictException.class, () ->
                idempotencyService.executeIdempotent(key, "TEST_RESOURCE", String.class, () -> "Should not run")
        );

        assertTrue(exception.getMessage().contains("đang được hệ thống xử lý"));
    }

    @Test
    @DisplayName("TH6 Test 4: Khi hàm nghiệp vụ ném lỗi, bản ghi được cập nhật sang FAILED")
    void testExecuteIdempotent_BusinessFailure_UpdatesToFailed() {
        String key = "test-key-004";

        assertThrows(RuntimeException.class, () ->
                idempotencyService.executeIdempotent(
                        key, "TEST_RESOURCE", String.class, () -> {
                            throw new RuntimeException("Simulated failure in business logic");
                        }
                )
        );

        IdempotencyRecord record = idempotencyRecordRepository.findByIdempotencyKey(key).orElse(null);
        assertNotNull(record);
        assertEquals(IdempotencyStatus.FAILED, record.getStatus());
        assertTrue(record.getResponseBody().contains("Simulated failure"));
    }

    @Test
    @DisplayName("TH6 Test 5: Không truyền Idempotency Key -> Thực thi bình thường không lưu cache")
    void testExecuteIdempotent_WithoutKey_ExecutesDirectly() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = idempotencyService.executeIdempotent(
                null, "TEST_RESOURCE", String.class, () -> {
                    counter.incrementAndGet();
                    return "Direct execution";
                }
        );

        assertEquals("Direct execution", result);
        assertEquals(1, counter.get());
        assertEquals(0, idempotencyRecordRepository.count());
    }
}
