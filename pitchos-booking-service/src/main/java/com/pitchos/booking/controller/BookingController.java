package com.pitchos.booking.controller;

import com.pitchos.booking.dto.BookingRequest;
import com.pitchos.booking.dto.BookingResponse;
import com.pitchos.booking.dto.PageResponse;
import com.pitchos.booking.dto.QuoteResponse;
import com.pitchos.booking.dto.UpdateStatusRequest;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.security.CurrentUser;
import com.pitchos.booking.service.BookingCommands.CreateOutcome;
import com.pitchos.booking.service.BookingSearchCriteria;
import com.pitchos.booking.service.BookingService;
import com.pitchos.booking.service.IdempotencyKeys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    static final int MAX_PAGE_SIZE = 100;
    private static final String BOOKINGS_PATH = "/api/v1/bookings/";

    private final BookingService bookingService;

    /**
     * Khách hàng đặt sân → 201 Created (+ Location). userId lấy từ JWT Token.
     * TH6: gửi lại với cùng header Idempotency-Key → 200 OK, trả lại đúng đơn cũ, header Idempotent-Replayed: true.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequest request) {
        CreateOutcome outcome = bookingService.createBooking(userId, request, idempotencyKey);
        URI location = URI.create(BOOKINGS_PATH + outcome.booking().getId());
        if (outcome.replayed()) {
            return ResponseEntity.ok().location(location)
                    .header(IdempotencyKeys.REPLAYED_HEADER, "true")
                    .body(outcome.booking());
        }
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(outcome.booking());
    }

    /** Danh sách đơn đặt của khách hàng hiện tại → 200 OK (giữ nguyên API cũ, không phân trang). */
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    /** TH3 - Danh sách có phân trang, sắp xếp, lọc theo trạng thái / sân / ngày / khoảng thời gian. */
    @GetMapping
    public ResponseEntity<PageResponse<BookingResponse>> searchBookings(
            Authentication authentication,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @Positive Long pitchId,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = BookingSort.DEFAULT) String sort) {
        BookingSearchCriteria criteria = BookingSearchCriteria.of(userId, pitchId, status, date, from, to);
        PageRequest pageable = PageRequest.of(page, size, BookingSort.parse(sort));
        return ResponseEntity.ok(bookingService.searchBookings(CurrentUser.from(authentication), criteria, pageable));
    }

    /** TH6 - Báo giá trước khi đặt (gọi Pitch Service lấy giá), không tạo đơn. */
    @GetMapping("/quote")
    public ResponseEntity<QuoteResponse> quote(
            @RequestParam @Positive Long pitchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(bookingService.quote(pitchId, startTime, endTime));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(Authentication authentication, @PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingService.getBooking(CurrentUser.from(authentication), id));
    }

    /** Khách sửa sân / giờ của đơn đang cho_duyet → 200 OK. */
    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(Authentication authentication,
                                                         @PathVariable("id") Long id,
                                                         @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.updateBooking(CurrentUser.from(authentication), id, request));
    }

    /** Hủy đơn (hủy mềm → da_huy, giữ lịch sử) → 200 OK kèm đơn sau khi hủy. */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(Authentication authentication, @PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(CurrentUser.from(authentication), id));
    }

    /** DELETE theo chuẩn REST = hủy mềm như /cancel (không xóa dòng khỏi CSDL) → 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(Authentication authentication, @PathVariable("id") Long id) {
        bookingService.cancelBooking(CurrentUser.from(authentication), id);
        return ResponseEntity.noContent().build();
    }

    /** Chủ sân duyệt / từ chối đơn → 200 OK (giữ nguyên API cũ). */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(Authentication authentication,
                                                        @PathVariable("id") Long id,
                                                        @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(bookingService.updateStatus(CurrentUser.from(authentication), id, request.getStatus()));
    }
}
