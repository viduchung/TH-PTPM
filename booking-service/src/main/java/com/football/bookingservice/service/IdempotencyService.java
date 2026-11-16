package com.football.bookingservice.service;

import com.football.bookingservice.entity.IdempotencyRecord;
import com.football.bookingservice.entity.IdempotencyStatus;
import com.football.bookingservice.exception.BadRequestException;
import com.football.bookingservice.exception.ConflictException;
import com.football.bookingservice.repository.IdempotencyRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * TH6 - Idempotency-Key chống tạo đơn trùng khi client gửi lại (mạng chập chờn, bấm 2 lần, retry tự động).
 * Vòng đời một key (bảng idempotency_keys): PROCESSING -> COMPLETED (kèm booking id) | FAILED (cho phép gửi lại).
 * Ràng buộc UNIQUE trên idempotency_key là chốt chặn cuối khi hai request cùng key chạy song song.
 */
@Slf4j
@Service
public class IdempotencyService {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";
    static final int MAX_KEY_LENGTH = 64;
    /** Quá thời gian này mà key vẫn PROCESSING thì coi như instance trước đã chết giữa chừng. */
    static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(1);
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1," + MAX_KEY_LENGTH + "}");

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /** Kết quả giành key: hoặc được xử lý mới, hoặc phải trả lại đơn đã tạo trước đó. */
    public record Claim(String storageKey, Long replayBookingId) {
        public boolean isReplay() {
            return replayBookingId != null;
        }
    }

    public static String validateKey(String rawKey) {
        String key = rawKey.trim();
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException(HEADER + " chỉ gồm chữ, số, '-' hoặc '_' và dài tối đa " + MAX_KEY_LENGTH + " ký tự");
        }
        return key;
    }

    /** Không đặt @Transactional: lỗi UNIQUE khi insert không được làm hỏng transaction của lần đọc lại phía sau. */
    public Claim claim(long userId, String validatedKey) {
        String storageKey = userId + ":" + validatedKey;
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(storageKey);
        if (existing.isEmpty()) {
            try {
                repository.saveAndFlush(new IdempotencyRecord(storageKey, IdempotencyRecord.RESOURCE_BOOKING_CREATION));
                return new Claim(storageKey, null);
            } catch (DataIntegrityViolationException race) {
                existing = repository.findByIdempotencyKey(storageKey);
                if (existing.isEmpty()) {
                    throw race;
                }
            }
        }
        IdempotencyRecord record = existing.get();
        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResourceId() != null) {
            return new Claim(storageKey, record.getResourceId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (repository.takeOver(storageKey, now, now.minus(PROCESSING_TIMEOUT)) == 1) {
            return new Claim(storageKey, null);
        }
        throw new ConflictException("Yêu cầu với " + HEADER + " này đang được xử lý, vui lòng thử lại sau giây lát");
    }

    /** Gọi TRONG transaction tạo đơn: đơn và trạng thái COMPLETED cùng commit hoặc cùng rollback. */
    @Transactional
    public void complete(String storageKey, long bookingId, String responseBody) {
        IdempotencyRecord record = repository.findByIdempotencyKey(storageKey)
                .orElseThrow(() -> new IllegalStateException("Mất bản ghi idempotency của request đang xử lý"));
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResourceId(bookingId);
        record.setResponseBody(responseBody);
    }

    /** Tạo đơn thất bại (trùng lịch, sân không tồn tại, pitchs-service lỗi...) -> client được gửi lại cùng key. */
    @Transactional
    public void markFailed(String storageKey) {
        repository.findByIdempotencyKey(storageKey).ifPresent(record -> {
            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                record.setStatus(IdempotencyStatus.FAILED);
            }
        });
    }
}
