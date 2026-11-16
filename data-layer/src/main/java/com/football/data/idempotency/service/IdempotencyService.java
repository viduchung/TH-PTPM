package com.football.data.idempotency.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.data.exception.ConflictException;
import com.football.data.idempotency.entity.IdempotencyRecord;
import com.football.data.idempotency.entity.IdempotencyStatus;
import com.football.data.idempotency.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Dịch vụ xử lý tính bất biến (Idempotency Service) chuẩn hóa Enterprise.
 * 
 * Đảm bảo:
 * 1. Khách hàng hoặc microservice retry request nhiều lần chỉ sinh ra duy nhất một kết quả.
 * 2. Request lặp lại sẽ nhận lại đúng kết quả đã hoàn thành trước đó (Cached Replay).
 * 3. Chặn tình trạng xử lý đồng thời (Concurrent Processing) với cùng một khóa.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * Thực thi nghiệp vụ với cơ chế đảm bảo tính bất biến (dựa theo Class)
     */
    public <T> T executeIdempotent(String idempotencyKey,
                                   String resourceType,
                                   Class<T> responseClass,
                                   Supplier<T> businessLogic) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return businessLogic.get();
        }

        Optional<IdempotencyRecord> existingRecordOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        if (existingRecordOpt.isPresent()) {
            IdempotencyRecord record = existingRecordOpt.get();

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info("[Idempotency] Khóa [{}] đã hoàn tất. Trả về kết quả lưu cache.", idempotencyKey);
                try {
                    return objectMapper.readValue(record.getResponseBody(), responseClass);
                } catch (Exception e) {
                    log.error("[Idempotency] Lỗi giải mã phản hồi cache: {}", e.getMessage());
                    throw new IllegalStateException("Không thể đọc kết quả lưu trữ của Idempotency Key", e);
                }
            } else if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                log.warn("[Idempotency] Khóa [{}] đang được xử lý song song.", idempotencyKey);
                throw new ConflictException("Yêu cầu này đang được hệ thống xử lý. Vui lòng không gửi lại liên tiếp!");
            } else {
                log.warn("[Idempotency] Khóa [{}] trước đó đã thất bại. Cho phép thực hiện lại.", idempotencyKey);
            }
        }

        // Tạo bản ghi mới ở trạng thái PROCESSING
        IdempotencyRecord record = existingRecordOpt.orElseGet(() -> IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .resourceType(resourceType)
                .status(IdempotencyStatus.PROCESSING)
                .build());

        record.setStatus(IdempotencyStatus.PROCESSING);
        record = idempotencyRecordRepository.save(record);

        try {
            T result = businessLogic.get();
            try {
                String responseJson = objectMapper.writeValueAsString(result);
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseBody(responseJson);
                idempotencyRecordRepository.save(record);
            } catch (Exception jsonEx) {
                log.error("[Idempotency] Không thể chuyển đổi kết quả sang JSON: {}", jsonEx.getMessage());
            }

            log.info("[Idempotency] Xử lý thành công và lưu kết quả cho khóa [{}]", idempotencyKey);
            return result;
        } catch (Exception ex) {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseBody(ex.getMessage());
            idempotencyRecordRepository.save(record);
            log.error("[Idempotency] Xử lý thất bại cho khóa [{}]: {}", idempotencyKey, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Thực thi nghiệp vụ với cơ chế đảm bảo tính bất biến (dựa theo TypeReference cho kiểu Generic)
     */
    public <T> T executeIdempotent(String idempotencyKey,
                                   String resourceType,
                                   TypeReference<T> typeReference,
                                   Supplier<T> businessLogic) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return businessLogic.get();
        }

        Optional<IdempotencyRecord> existingRecordOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        if (existingRecordOpt.isPresent()) {
            IdempotencyRecord record = existingRecordOpt.get();

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info("[Idempotency] Khóa [{}] đã hoàn tất. Trả về kết quả lưu cache.", idempotencyKey);
                try {
                    return objectMapper.readValue(record.getResponseBody(), typeReference);
                } catch (Exception e) {
                    log.error("[Idempotency] Lỗi giải mã phản hồi cache: {}", e.getMessage());
                    throw new IllegalStateException("Không thể đọc kết quả lưu trữ của Idempotency Key", e);
                }
            } else if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new ConflictException("Yêu cầu này đang được hệ thống xử lý. Vui lòng không gửi lại liên tiếp!");
            }
        }

        IdempotencyRecord record = existingRecordOpt.orElseGet(() -> IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .resourceType(resourceType)
                .status(IdempotencyStatus.PROCESSING)
                .build());

        record.setStatus(IdempotencyStatus.PROCESSING);
        record = idempotencyRecordRepository.save(record);

        try {
            T result = businessLogic.get();
            try {
                String responseJson = objectMapper.writeValueAsString(result);
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseBody(responseJson);
                idempotencyRecordRepository.save(record);
            } catch (Exception jsonEx) {
                log.error("[Idempotency] Không thể chuyển đổi kết quả sang JSON: {}", jsonEx.getMessage());
            }

            return result;
        } catch (Exception ex) {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseBody(ex.getMessage());
            idempotencyRecordRepository.save(record);
            throw ex;
        }
    }
}
