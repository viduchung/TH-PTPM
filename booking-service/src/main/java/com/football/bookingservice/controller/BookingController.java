package com.football.bookingservice.controller;

import com.football.bookingservice.dto.ApiResponse;
import com.football.bookingservice.dto.BookingRequest;
import com.football.bookingservice.dto.BookingResponse;
import com.football.bookingservice.dto.BookingStatusUpdateRequest;
import com.football.bookingservice.dto.PageResponse;
import com.football.bookingservice.dto.PitchScheduleResponse;
import com.football.bookingservice.dto.QuoteResponse;
import com.football.bookingservice.entity.BookingStatus;
import com.football.bookingservice.security.AuthenticatedUser;
import com.football.bookingservice.service.BookingSearchCriteria;
import com.football.bookingservice.service.BookingService;
import com.football.bookingservice.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API đặt sân (TH1, TH3, TH5, TH6). Controller mỏng: nhận request, gọi BookingService, bọc ApiResponse.
 * Phân quyền theo vai trò ở đây (@PreAuthorize); quyền sở hữu đơn kiểm tra trong service.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Booking", description = "Đặt sân, xem lịch, duyệt đơn, báo giá")
public class BookingController {

    static final int MAX_PAGE_SIZE = 100;
    private static final String BOOKINGS_PATH = "/api/v1/bookings/";

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Khách hàng đặt sân (201). Gửi lại cùng header Idempotency-Key -> 200 + đơn cũ")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = IdempotencyService.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequest request) {
        BookingService.CreateOutcome outcome = bookingService.create(user, request, idempotencyKey);
        URI location = URI.create(BOOKINGS_PATH + outcome.booking().id());
        if (outcome.replayed()) {
            return ResponseEntity.ok().location(location)
                    .header(IdempotencyService.REPLAYED_HEADER, "true")
                    .body(ApiResponse.ok("Đơn đã được tạo trước đó với cùng Idempotency-Key", outcome.booking()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).location(location)
                .body(ApiResponse.ok("Đặt sân thành công, đơn đang chờ duyệt", outcome.booking()));
    }

    @Operation(summary = "Danh sách đơn có phân trang & lọc (khách hàng chỉ thấy đơn của mình)")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ApiResponse<PageResponse<BookingResponse>> search(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @Positive Long pitchId,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = BookingSort.DEFAULT) String sort) {
        var criteria = new BookingSearchCriteria(userId, pitchId, status, date, from, to);
        var pageable = PageRequest.of(page, size, BookingSort.parse(sort));
        return ApiResponse.ok("Thành công", bookingService.search(user, criteria, pageable));
    }

    @Operation(summary = "Đơn của chính tôi (khách hàng), mới đặt trước")
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PageResponse<BookingResponse>> myBookings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        var criteria = new BookingSearchCriteria(null, null, status, null, null, null);
        var pageable = PageRequest.of(page, size, BookingSort.parse("createdAt,desc"));
        return ApiResponse.ok("Thành công", bookingService.search(user, criteria, pageable));
    }

    @Operation(summary = "Báo giá trước khi đặt + kiểm tra khung giờ còn trống (công khai, không tạo đơn)")
    @GetMapping("/quote")
    public ApiResponse<QuoteResponse> quote(
            @RequestParam @Positive Long pitchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.ok("Thành công", bookingService.quote(pitchId, startTime, endTime));
    }

    @Operation(summary = "Lịch đã kín của một sân trong ngày (công khai)")
    @GetMapping("/pitches/{pitchId}/schedule")
    public ApiResponse<PitchScheduleResponse> schedule(
            @PathVariable @Positive Long pitchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok("Thành công", bookingService.schedule(pitchId, date));
    }

    @Operation(summary = "Chi tiết một đơn (khách hàng chỉ xem được đơn của mình)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'ADMIN', 'INTERNAL_SERVICE')")
    public ApiResponse<BookingResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable @Positive Long id) {
        return ApiResponse.ok("Thành công", bookingService.get(user, id));
    }

    @Operation(summary = "Khách đổi sân / giờ của đơn đang chờ duyệt")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<BookingResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable @Positive Long id,
                                               @Valid @RequestBody BookingRequest request) {
        return ApiResponse.ok("Cập nhật đơn thành công", bookingService.update(user, id, request));
    }

    @Operation(summary = "Hủy đơn (hủy mềm -> DA_HUY, giữ lịch sử)")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<BookingResponse> cancel(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable @Positive Long id) {
        return ApiResponse.ok("Đã hủy đơn", bookingService.cancel(user, id));
    }

    @Operation(summary = "DELETE theo chuẩn REST = hủy mềm như /cancel (204)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable @Positive Long id) {
        bookingService.cancel(user, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Chủ sân / admin duyệt (DA_DUYET) hoặc từ chối (TU_CHOI) đơn")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ApiResponse<BookingResponse> review(@PathVariable @Positive Long id,
                                               @Valid @RequestBody BookingStatusUpdateRequest request) {
        return ApiResponse.ok("Cập nhật trạng thái đơn thành công", bookingService.review(id, request.status()));
    }
}
