package com.football.bookingservice.service;

import com.football.bookingservice.entity.Booking;
import com.football.bookingservice.entity.BookingStatus;
import com.football.bookingservice.exception.BadRequestException;
import com.football.bookingservice.exception.ConflictException;
import com.football.bookingservice.exception.ForbiddenException;
import com.football.bookingservice.exception.ResourceNotFoundException;
import com.football.bookingservice.repository.BookingRepository;
import com.football.bookingservice.repository.PitchLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * TH4 - Mọi thao tác GHI, mỗi phương thức là MỘT transaction ngắn, KHÔNG gọi service khác bên trong.
 * READ_COMMITTED: sau khi chờ được khóa sân, câu kiểm tra trùng lịch thấy ngay đơn mà transaction trước vừa commit.
 * Các kiểm tra quyền / trạng thái được làm lại SAU khi khóa dòng, vì trạng thái có thể vừa bị request khác đổi.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BookingWriter {

    static final String SLOT_TAKEN = "Khung giờ này đã có người đặt, vui lòng chọn giờ khác";
    static final String NOT_YOUR_BOOKING = "Bạn chỉ được thao tác trên đơn đặt sân của chính mình";

    private final BookingRepository bookingRepository;
    private final PitchLockRepository pitchLockRepository;
    private final IdempotencyService idempotencyService;
    private final BookingTimeRules timeRules;

    public BookingWriter(BookingRepository bookingRepository, PitchLockRepository pitchLockRepository,
                         IdempotencyService idempotencyService, BookingTimeRules timeRules) {
        this.bookingRepository = bookingRepository;
        this.pitchLockRepository = pitchLockRepository;
        this.idempotencyService = idempotencyService;
        this.timeRules = timeRules;
    }

    /** Khóa sân -> kiểm tra trùng lịch -> lưu đơn CHO_DUYET (+ chốt Idempotency-Key). Khóa nhả khi commit. */
    public Booking insert(long userId, PricedSlot slot, String note, String idempotencyStorageKey) {
        lockPitch(slot.pitchId());
        requireSlotFree(slot, null);
        Booking booking = bookingRepository.saveAndFlush(Booking.builder()
                .userId(userId)
                .pitchId(slot.pitchId())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .pricePerHour(slot.pricePerHour())
                .totalPrice(slot.totalPrice())
                .status(BookingStatus.CHO_DUYET)
                .note(note)
                .build());
        if (idempotencyStorageKey != null) {
            idempotencyService.complete(idempotencyStorageKey, booking.getId(),
                    "{\"bookingId\":" + booking.getId() + "}");
        }
        return booking;
    }

    /** Khách đổi sân / giờ của đơn còn CHO_DUYET; giá đã được tính lại từ pitchs-service. */
    public Booking reschedule(long userId, Long bookingId, PricedSlot slot, String note) {
        Booking booking = lockBooking(bookingId);
        requireEditableBy(userId, booking);
        lockPitch(slot.pitchId());
        requireSlotFree(slot, bookingId);
        booking.setPitchId(slot.pitchId());
        booking.setStartTime(slot.startTime());
        booking.setEndTime(slot.endTime());
        booking.setPricePerHour(slot.pricePerHour());
        booking.setTotalPrice(slot.totalPrice());
        booking.setNote(note);
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * Hủy mềm (DA_HUY): giữ lịch sử, nhả khung giờ. Khách chỉ hủy đơn của mình và trước giờ đá;
     * actorUserId = null nghĩa là ADMIN (được hủy mọi đơn chưa kết thúc).
     */
    public Booking cancel(Long bookingId, Long actorUserId) {
        Booking booking = lockBooking(bookingId);
        if (actorUserId != null) {
            if (!booking.getUserId().equals(actorUserId)) {
                throw new ForbiddenException(NOT_YOUR_BOOKING);
            }
            if (timeRules.hasStarted(booking.getStartTime())) {
                throw new BadRequestException("Không thể hủy đơn đã tới giờ đá");
            }
        }
        return transition(booking, BookingStatus.DA_HUY);
    }

    /** Chủ sân / admin duyệt hoặc từ chối. */
    public Booking review(Long bookingId, BookingStatus target) {
        return transition(lockBooking(bookingId), target);
    }

    private Booking transition(Booking booking, BookingStatus target) {
        if (!booking.getStatus().canTransitionTo(target)) {
            throw new ConflictException("Không thể chuyển đơn từ " + booking.getStatus() + " sang " + target);
        }
        booking.setStatus(target);
        return bookingRepository.saveAndFlush(booking);
    }

    static void requireEditableBy(long userId, Booking booking) {
        if (!booking.getUserId().equals(userId)) {
            throw new ForbiddenException(NOT_YOUR_BOOKING);
        }
        if (booking.getStatus() != BookingStatus.CHO_DUYET) {
            throw new ConflictException("Chỉ sửa được đơn đang chờ duyệt (CHO_DUYET)");
        }
    }

    private void requireSlotFree(PricedSlot slot, Long excludedBookingId) {
        long overlapping = bookingRepository.countOverlapping(slot.pitchId(), slot.startTime(), slot.endTime(),
                excludedBookingId, BookingStatus.releasedStatuses());
        if (overlapping > 0) {
            throw new ConflictException(SLOT_TAKEN);
        }
    }

    private Booking lockBooking(Long bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> bookingNotFound(bookingId));
    }

    private void lockPitch(Long pitchId) {
        pitchLockRepository.lockByPitchId(pitchId)
                .orElseThrow(() -> new IllegalStateException("Thiếu dòng khóa cho sân " + pitchId));
    }

    /** Dùng chung thông điệp 404 cho BookingWriter và BookingService. */
    static ResourceNotFoundException bookingNotFound(Long bookingId) {
        return new ResourceNotFoundException("Không tìm thấy đơn đặt sân id = " + bookingId);
    }
}
