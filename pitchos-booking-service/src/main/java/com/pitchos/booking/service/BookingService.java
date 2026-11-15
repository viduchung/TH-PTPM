package com.pitchos.booking.service;

import com.pitchos.booking.client.PitchGateway;
import com.pitchos.booking.dto.BookingRequest;
import com.pitchos.booking.dto.BookingResponse;
import com.pitchos.booking.dto.PageResponse;
import com.pitchos.booking.dto.PitchResponseDTO;
import com.pitchos.booking.dto.QuoteResponse;
import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.exception.BadRequestException;
import com.pitchos.booking.exception.ForbiddenException;
import com.pitchos.booking.repository.BookingRepository;
import com.pitchos.booking.security.CurrentUser;
import com.pitchos.booking.security.JsonSecurityHandlers;
import com.pitchos.booking.service.BookingCommands.CreateOutcome;
import com.pitchos.booking.service.BookingCommands.NewBooking;
import com.pitchos.booking.service.BookingCommands.PricedSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Điều phối nghiệp vụ đặt sân. Lời gọi Pitch Service (mạng, có thể chậm) luôn nằm NGOÀI transaction;
 * phần ghi CSDL nằm trong BookingWriter (transaction ngắn, có khóa theo sân).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Set<BookingStatus> REVIEW_STATUSES = Set.of(BookingStatus.da_duyet, BookingStatus.tu_choi);

    private final BookingRepository bookingRepository;
    private final PitchGateway pitchGateway;
    private final PitchLockService pitchLockService;
    private final BookingWriter bookingWriter;
    private final BookingTimeRules timeRules;
    private final BookingAccessPolicy accessPolicy;

    /** Tương thích chữ ký cũ (không có Idempotency-Key). */
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        return createBooking(userId, request, null).booking();
    }

    /**
     * Đặt sân: (gửi lại cùng key → trả đơn cũ) → kiểm tra giờ → gọi Pitch Service lấy giá + trạng thái
     * → khóa sân, kiểm tra trùng lịch, lưu đơn cho_duyet. Pitch không xác minh được → 404/503, KHÔNG tạo đơn.
     */
    public CreateOutcome createBooking(Long userId, BookingRequest request, String rawIdempotencyKey) {
        String key = IdempotencyKeys.normalize(rawIdempotencyKey);
        if (key != null) {
            var existing = bookingRepository.findByUserIdAndIdempotencyKey(userId, key);
            if (existing.isPresent()) {
                return BookingWriter.replayOrConflict(existing.get(), requestedSlot(request));
            }
        }
        timeRules.validate(request.getStartTime(), request.getEndTime());
        PricedSlot slot = priceSlot(request.getPitchId(), request.getStartTime(), request.getEndTime());

        pitchLockService.ensureLockRow(slot.pitchId());
        try {
            return bookingWriter.insert(new NewBooking(userId, slot, key));
        } catch (DataIntegrityViolationException ex) {
            if (key == null) {
                throw ex;
            }
            // Hai request cùng key chạy song song trên 2 sân khác nhau: ràng buộc UNIQUE chặn request sau
            Booking winner = bookingRepository.findByUserIdAndIdempotencyKey(userId, key).orElseThrow(() -> ex);
            return BookingWriter.replayOrConflict(winner, slot);
        }
    }

    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /**
     * user chỉ thấy đơn của mình (bỏ qua userId client gửi); chusan theo BookingAccessPolicy; admin thấy tất cả.
     * Role không thuộc 3 role trên (ví dụ "guest", "CUSTOMER") → 403: không bao giờ mặc định coi như admin.
     */
    public PageResponse<BookingResponse> searchBookings(CurrentUser user, BookingSearchCriteria criteria,
                                                        Pageable pageable) {
        BookingSearchCriteria scoped = criteria;
        if (user.isCustomer()) {
            scoped = criteria.withUserId(user.userId());
        } else if (user.isOwner()) {
            accessPolicy.requireOwnerMayListAll();
        } else if (!user.isAdmin()) {
            throw new ForbiddenException(JsonSecurityHandlers.FORBIDDEN_MESSAGE);
        }
        return PageResponse.from(bookingRepository.findAll(scoped.toSpecification(), pageable), BookingResponse::from);
    }

    public BookingResponse getBooking(CurrentUser user, Long bookingId) {
        Booking booking = bookingWriter.findOrThrow(bookingId);
        accessPolicy.requireCanView(user, booking);
        return BookingResponse.from(booking);
    }

    /** PUT: khách đổi sân / giờ của đơn còn cho_duyet; giá được tính lại từ Pitch Service. */
    public BookingResponse updateBooking(CurrentUser user, Long bookingId, BookingRequest request) {
        BookingWriter.requireEditableByCustomer(user, bookingWriter.findOrThrow(bookingId));
        timeRules.validate(request.getStartTime(), request.getEndTime());
        PricedSlot slot = priceSlot(request.getPitchId(), request.getStartTime(), request.getEndTime());

        pitchLockService.ensureLockRow(slot.pitchId());
        return BookingResponse.from(bookingWriter.reschedule(user, bookingId, slot));
    }

    public BookingResponse cancelBooking(CurrentUser user, Long bookingId) {
        return BookingResponse.from(bookingWriter.cancel(user, bookingId));
    }

    /** Chủ sân duyệt (da_duyet) hoặc từ chối (tu_choi) đơn; chủ sân phải sở hữu sân (khi Pitch cung cấp ownerId). */
    public BookingResponse updateStatus(CurrentUser user, Long bookingId, BookingStatus status) {
        if (!REVIEW_STATUSES.contains(status)) {
            throw new BadRequestException("status chỉ được là da_duyet hoặc tu_choi");
        }
        Booking booking = bookingWriter.findOrThrow(bookingId);
        if (user.isOwner()) {
            accessPolicy.requireOwnsPitch(user, booking.getPitchId());
        }
        return BookingResponse.from(bookingWriter.changeStatus(bookingId, status));
    }

    /** TH6 - Báo giá: Booking (Consumer) lấy giá từ Pitch Service (Provider), KHÔNG tạo đơn. */
    public QuoteResponse quote(Long pitchId, LocalDateTime startTime, LocalDateTime endTime) {
        timeRules.validate(startTime, endTime);
        PitchResponseDTO pitch = requireActivePitch(pitchId);
        boolean isAvailable = bookingRepository.countOverlappingBookings(pitchId, startTime, endTime) == 0;
        return new QuoteResponse(pitchId, pitch.getName(), pitch.getPricePerHour(), startTime, endTime,
                BookingTimeRules.durationMinutes(startTime, endTime),
                BookingTimeRules.totalPrice(pitch.getPricePerHour(), startTime, endTime),
                isAvailable);
    }

    private PricedSlot priceSlot(Long pitchId, LocalDateTime startTime, LocalDateTime endTime) {
        PitchResponseDTO pitch = requireActivePitch(pitchId);
        return new PricedSlot(pitchId, startTime, endTime, pitch.getPricePerHour(),
                BookingTimeRules.totalPrice(pitch.getPricePerHour(), startTime, endTime));
    }

    private PitchResponseDTO requireActivePitch(Long pitchId) {
        PitchResponseDTO pitch = pitchGateway.getPitch(pitchId);
        log.info("Pitch Service trả về sân {}: giá {} /giờ, trạng thái {}",
                pitch.getId(), pitch.getPricePerHour(), pitch.getStatus());
        if (!pitch.isActive()) {
            throw new BadRequestException("Sân đang bảo trì, không thể đặt");
        }
        return pitch;
    }

    private static PricedSlot requestedSlot(BookingRequest request) {
        return new PricedSlot(request.getPitchId(), request.getStartTime(), request.getEndTime(), null, null);
    }
}
