package com.football.bookingservice.service;

import com.football.bookingservice.client.PitchClient;
import com.football.bookingservice.client.dto.PitchInfo;
import com.football.bookingservice.client.dto.PitchQuote;
import com.football.bookingservice.dto.BookingRequest;
import com.football.bookingservice.dto.BookingResponse;
import com.football.bookingservice.dto.PageResponse;
import com.football.bookingservice.dto.PitchScheduleResponse;
import com.football.bookingservice.dto.QuoteResponse;
import com.football.bookingservice.entity.Booking;
import com.football.bookingservice.entity.BookingStatus;
import com.football.bookingservice.exception.BadRequestException;
import com.football.bookingservice.exception.ConflictException;
import com.football.bookingservice.exception.ForbiddenException;
import com.football.bookingservice.repository.BookingRepository;
import com.football.bookingservice.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Điều phối nghiệp vụ đặt sân. Lời gọi mạng (pitchs-service, auth-service) luôn nằm NGOÀI transaction;
 * phần ghi CSDL nằm trong BookingWriter (transaction ngắn, có khóa theo sân).
 */
@Slf4j
@Service
public class BookingService {

    private static final Set<BookingStatus> REVIEW_TARGETS = EnumSet.of(BookingStatus.DA_DUYET, BookingStatus.TU_CHOI);
    private static final int MONEY_SCALE = 2;

    private final BookingRepository bookingRepository;
    private final BookingWriter bookingWriter;
    private final PitchClient pitchClient;
    private final PitchLockService pitchLockService;
    private final IdempotencyService idempotencyService;
    private final UserIdResolver userIdResolver;
    private final BookingTimeRules timeRules;

    public BookingService(BookingRepository bookingRepository, BookingWriter bookingWriter, PitchClient pitchClient,
                          PitchLockService pitchLockService, IdempotencyService idempotencyService,
                          UserIdResolver userIdResolver, BookingTimeRules timeRules) {
        this.bookingRepository = bookingRepository;
        this.bookingWriter = bookingWriter;
        this.pitchClient = pitchClient;
        this.pitchLockService = pitchLockService;
        this.idempotencyService = idempotencyService;
        this.userIdResolver = userIdResolver;
        this.timeRules = timeRules;
    }

    public record CreateOutcome(BookingResponse booking, boolean replayed) {
    }

    /**
     * Đặt sân: (gửi lại cùng Idempotency-Key -> trả đơn cũ) -> kiểm tra giờ -> pitchs-service xác nhận sân + báo giá
     * -> khóa sân, kiểm tra trùng lịch, lưu đơn CHO_DUYET. pitchs-service lỗi -> KHÔNG tạo đơn.
     */
    public CreateOutcome create(AuthenticatedUser user, BookingRequest request, String rawIdempotencyKey) {
        String validatedKey = rawIdempotencyKey == null ? null : IdempotencyService.validateKey(rawIdempotencyKey);
        long userId = userIdResolver.requireUserId(user);
        timeRules.validate(request.startTime(), request.endTime());

        String storageKey = null;
        if (validatedKey != null) {
            IdempotencyService.Claim claim = idempotencyService.claim(userId, validatedKey);
            if (claim.isReplay()) {
                return replay(claim.replayBookingId(), request);
            }
            storageKey = claim.storageKey();
        }
        try {
            PricedSlot slot = priceSlot(request.pitchId(), request.startTime(), request.endTime());
            pitchLockService.ensureLockRow(slot.pitchId());
            Booking booking = bookingWriter.insert(userId, slot, normalizeNote(request.note()), storageKey);
            log.info("Tạo đơn {} cho user {} tại sân {} ({} - {})", booking.getId(), userId, slot.pitchId(),
                    slot.startTime(), slot.endTime());
            return new CreateOutcome(BookingResponse.from(booking, slot.pitchName()), false);
        } catch (RuntimeException ex) {
            if (storageKey != null) {
                idempotencyService.markFailed(storageKey);
            }
            throw ex;
        }
    }

    /** Khách hàng chỉ thấy đơn của mình (bỏ qua userId client gửi); chủ sân, admin, service nội bộ thấy tất cả. */
    public PageResponse<BookingResponse> search(AuthenticatedUser user, BookingSearchCriteria criteria,
                                                Pageable pageable) {
        BookingSearchCriteria scoped = user.isCustomer()
                ? criteria.withUserId(userIdResolver.requireUserId(user))
                : criteria;
        return PageResponse.from(bookingRepository.findAll(scoped.toSpecification(), pageable), BookingResponse::from);
    }

    public BookingResponse get(AuthenticatedUser user, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BookingWriter.bookingNotFound(bookingId));
        if (user.isCustomer() && !booking.getUserId().equals(userIdResolver.requireUserId(user))) {
            throw new ForbiddenException(BookingWriter.NOT_YOUR_BOOKING);
        }
        return BookingResponse.from(booking);
    }

    /** PUT: khách đổi sân / giờ của đơn CHO_DUYET; giá được tính lại từ pitchs-service. */
    public BookingResponse update(AuthenticatedUser user, Long bookingId, BookingRequest request) {
        long userId = userIdResolver.requireUserId(user);
        Booking current = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BookingWriter.bookingNotFound(bookingId));
        // Kiểm tra sớm để khỏi gọi pitchs-service vô ích; BookingWriter kiểm tra lại sau khi khóa dòng.
        BookingWriter.requireEditableBy(userId, current);
        timeRules.validate(request.startTime(), request.endTime());

        PricedSlot slot = priceSlot(request.pitchId(), request.startTime(), request.endTime());
        pitchLockService.ensureLockRow(slot.pitchId());
        Booking updated = bookingWriter.reschedule(userId, bookingId, slot, normalizeNote(request.note()));
        return BookingResponse.from(updated, slot.pitchName());
    }

    public BookingResponse cancel(AuthenticatedUser user, Long bookingId) {
        if (user.isAdmin()) {
            return BookingResponse.from(bookingWriter.cancel(bookingId, null));
        }
        if (!user.isCustomer()) {
            throw new ForbiddenException("Chỉ khách đặt sân hoặc quản trị viên được hủy đơn");
        }
        return BookingResponse.from(bookingWriter.cancel(bookingId, userIdResolver.requireUserId(user)));
    }

    /**
     * Chủ sân / admin duyệt (DA_DUYET) hoặc từ chối (TU_CHOI).
     * Hạn chế đã biết: pitch_db chưa lưu chủ của từng sân, nên mọi OWNER duyệt được mọi đơn.
     */
    public BookingResponse review(Long bookingId, BookingStatus target) {
        if (!REVIEW_TARGETS.contains(target)) {
            throw new BadRequestException("status chỉ được là DA_DUYET hoặc TU_CHOI (hủy đơn dùng /cancel)");
        }
        return BookingResponse.from(bookingWriter.review(bookingId, target));
    }

    /** TH6 - Báo giá: booking (Consumer) hỏi giá pitchs-service (Provider) và tự kiểm tra lịch trống, KHÔNG tạo đơn. */
    public QuoteResponse quote(Long pitchId, LocalDateTime startTime, LocalDateTime endTime) {
        timeRules.validate(startTime, endTime);
        PricedSlot slot = priceSlot(pitchId, startTime, endTime);
        boolean isAvailable = bookingRepository.countOverlapping(pitchId, startTime, endTime, null,
                BookingStatus.releasedStatuses()) == 0;
        return new QuoteResponse(pitchId, slot.pitchName(), startTime, endTime, slot.totalMinutes(),
                slot.pricePerHour(), slot.totalPrice(), slot.goldenHourApplied(), slot.calculationBreakdown(),
                isAvailable);
    }

    /** TH1 - Lịch đã kín của một sân trong ngày (công khai, không lộ thông tin người đặt). */
    public PitchScheduleResponse schedule(Long pitchId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        var busySlots = bookingRepository
                .findByPitchIdAndStatusNotInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                        pitchId, BookingStatus.releasedStatuses(), dayStart.plusDays(1), dayStart)
                .stream()
                .map(b -> new PitchScheduleResponse.BusySlot(b.getStartTime(), b.getEndTime(), b.getStatus().name()))
                .toList();
        return new PitchScheduleResponse(pitchId, date, busySlots);
    }

    private CreateOutcome replay(Long bookingId, BookingRequest request) {
        Booking existing = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Idempotency-Key trỏ tới đơn không còn tồn tại"));
        boolean isSameRequest = Objects.equals(existing.getPitchId(), request.pitchId())
                && Objects.equals(existing.getStartTime(), request.startTime())
                && Objects.equals(existing.getEndTime(), request.endTime());
        if (!isSameRequest) {
            throw new ConflictException(IdempotencyService.HEADER
                    + " này đã được dùng cho một yêu cầu đặt sân khác (khác sân hoặc khác giờ)");
        }
        return new CreateOutcome(BookingResponse.from(existing), true);
    }

    private PricedSlot priceSlot(Long pitchId, LocalDateTime startTime, LocalDateTime endTime) {
        PitchInfo pitch = pitchClient.getPitch(pitchId);
        if (pitch.isUnderMaintenance()) {
            throw new BadRequestException("Sân '" + pitch.name() + "' đang bảo dưỡng, không thể đặt");
        }
        PitchQuote quote = pitchClient.getQuote(pitchId, startTime.toLocalDate(),
                startTime.toLocalTime(), endTime.toLocalTime());
        return new PricedSlot(pitchId, pitch.name(), startTime, endTime, quote.totalMinutes(),
                quote.basePricePerHour(), quote.totalAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                quote.goldenHourApplied(), quote.calculationBreakdown());
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
