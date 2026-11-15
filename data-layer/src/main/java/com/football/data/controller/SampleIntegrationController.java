package com.football.data.controller;

import com.football.data.booking.entity.BookingStatus;
import com.football.data.consistency.BookingStateTransitionValidator;
import com.football.data.consistency.DistributedBookingSagaOrchestrator;
import com.football.data.dto.booking.BookingRequestDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.dto.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller kiểm thử tích hợp liên dịch vụ (TH6).
 * Minh họa điều phối giao dịch phân tán (Saga Orchestration) và tính bất biến (Idempotency Key).
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class SampleIntegrationController {

    private final DistributedBookingSagaOrchestrator sagaOrchestrator;
    private final BookingStateTransitionValidator stateValidator;

    /**
     * Endpoint điều phối Saga: Tạo đơn đặt sân, kiểm tra trùng lịch, thanh toán và xử lý bù trừ
     */
    @PostMapping("/saga/book-and-pay")
    public ResponseEntity<ApiResponse<BookingResponseDTO>> executeBookingSaga(
            @Valid @RequestBody BookingRequestDTO request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(value = "paymentMethod", defaultValue = "VNPAY") String paymentMethod) {

        BookingResponseDTO response = sagaOrchestrator.executeBookingSaga(request, paymentMethod, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thực thi chu trình Saga thành công"));
    }

    /**
     * Endpoint kiểm chứng quy tắc chuyển đổi trạng thái (State Transition Check)
     */
    @PostMapping("/validate-state")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateStateTransition(
            @RequestParam("currentStatus") BookingStatus currentStatus,
            @RequestParam("targetStatus") BookingStatus targetStatus) {

        stateValidator.validateTransition(currentStatus, targetStatus);

        Map<String, Object> result = Map.of(
                "currentStatus", currentStatus,
                "targetStatus", targetStatus,
                "valid", true,
                "message", "Chuyển đổi trạng thái hợp lệ theo quy trình nghiệp vụ"
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Xác thực trạng thái thành công"));
    }
}

