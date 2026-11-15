package com.pitchos.booking.service;

import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.entity.BookingStatus;
import com.pitchos.booking.exception.BadRequestException;
import com.pitchos.booking.exception.ConflictException;
import com.pitchos.booking.exception.ForbiddenException;
import com.pitchos.booking.exception.ResourceNotFoundException;
import com.pitchos.booking.repository.BookingRepository;
import com.pitchos.booking.repository.PitchLockRepository;
import com.pitchos.booking.security.CurrentUser;
import com.pitchos.booking.service.BookingCommands.CreateOutcome;
import com.pitchos.booking.service.BookingCommands.NewBooking;
import com.pitchos.booking.service.BookingCommands.PricedSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * TH4 - Các thao tác GHI, mỗi phương thức là MỘT transaction ngắn (không gọi service khác bên trong).
 * READ_COMMITTED: sau khi chờ được khóa sân, câu kiểm tra trùng lịch đọc được đơn mà transaction trước vừa commit
 * (với REPEATABLE READ mặc định của MySQL, ảnh chụp dữ liệu có thể đã cũ).
 */
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BookingWriter {

    static final String SLOT_TAKEN = "Khung giờ này đã có người đặt";
    static final String KEY_REUSED =
            "Idempotency-Key này đã được dùng cho một yêu cầu đặt sân khác (khác sân hoặc khác giờ)";

    private final BookingRepository bookingRepository;
    private final PitchLockRepository pitchLockRepository;
    private final BookingTimeRules timeRules;

    /** Khóa sân → (kiểm tra key) → kiểm tra trùng lịch → lưu đơn cho_duyet. Khóa nhả khi commit. */
    public CreateOutcome insert(NewBooking command) {
        PricedSlot slot = command.slot();
        lockPitch(slot.pitchId());

        if (command.idempotencyKey() != null) {
            var existing = bookingRepository.findByUserIdAndIdempotencyKey(command.userId(), command.idempotencyKey());
            if (existing.isPresent()) {
                return replayOrConflict(existing.get(), slot);
            }
        }
        if (bookingRepository.countOverlappingBookings(slot.pitchId(), slot.startTime(), slot.endTime()) > 0) {
            throw new BadRequestException(SLOT_TAKEN);
        }

        Booking booking = Booking.builder()
                .userId(command.userId())
                .pitchId(slot.pitchId())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .pricePerHour(slot.pricePerHour())
                .totalPrice(slot.totalPrice())
                .status(BookingStatus.cho_duyet)
                .idempotencyKey(command.idempotencyKey())
                .build();
        return CreateOutcome.created(bookingRepository.saveAndFlush(booking));
    }

    /** Khách sửa giờ / sân của đơn còn cho_duyet. Kiểm tra lại quyền và trạng thái SAU khi đọc trong transaction. */
    public Booking reschedule(CurrentUser user, Long bookingId, PricedSlot slot) {
        Booking booking = findOrThrow(bookingId);
        requireEditableByCustomer(user, booking);
        lockPitch(slot.pitchId());

        if (bookingRepository.countOverlappingBookingsExcluding(
                slot.pitchId(), slot.startTime(), slot.endTime(), bookingId) > 0) {
            throw new BadRequestException(SLOT_TAKEN);
        }
        booking.setPitchId(slot.pitchId());
        booking.setStartTime(slot.startTime());
        booking.setEndTime(slot.endTime());
        booking.setPricePerHour(slot.pricePerHour());
        booking.setTotalPrice(slot.totalPrice());
        return bookingRepository.saveAndFlush(booking);
    }

    /** Hủy mềm (da_huy): giữ lại lịch sử đặt sân, nhả khung giờ cho người khác. */
    public Booking cancel(CurrentUser user, Long bookingId) {
        Booking booking = findOrThrow(bookingId);
        if (!user.isAdmin()) {
            if (!booking.getUserId().equals(user.userId())) {
                throw new ForbiddenException(BookingAccessPolicy.NOT_YOUR_BOOKING);
            }
            if (timeRules.hasStarted(booking.getStartTime())) {
                throw new BadRequestException("Không thể hủy đơn đã tới giờ đá");
            }
        }
        requireNotFinal(booking);
        booking.setStatus(BookingStatus.da_huy);
        return bookingRepository.saveAndFlush(booking);
    }

    /** Chủ sân / admin duyệt hoặc từ chối (quyền chủ sân đã kiểm tra trước ở BookingAccessPolicy). */
    public Booking changeStatus(Long bookingId, BookingStatus status) {
        Booking booking = findOrThrow(bookingId);
        requireNotFinal(booking);
        booking.setStatus(status);
        return bookingRepository.saveAndFlush(booking);
    }

    @Transactional(readOnly = true)
    public Booking findOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân id = " + bookingId));
    }

    static void requireEditableByCustomer(CurrentUser user, Booking booking) {
        if (!booking.getUserId().equals(user.userId())) {
            throw new ForbiddenException(BookingAccessPolicy.NOT_YOUR_BOOKING);
        }
        if (booking.getStatus() != BookingStatus.cho_duyet) {
            throw new BadRequestException("Chỉ sửa được đơn đang chờ duyệt (cho_duyet)");
        }
    }

    static CreateOutcome replayOrConflict(Booking existing, PricedSlot slot) {
        boolean isSameRequest = Objects.equals(existing.getPitchId(), slot.pitchId())
                && Objects.equals(existing.getStartTime(), slot.startTime())
                && Objects.equals(existing.getEndTime(), slot.endTime());
        if (!isSameRequest) {
            throw new ConflictException(KEY_REUSED);
        }
        return CreateOutcome.replayed(existing);
    }

    /**
     * Đơn tu_choi / da_huy đã nhả khung giờ (truy vấn trùng lịch bỏ qua chúng), có thể người khác đã đặt vào
     * → không cho đổi trạng thái nữa, tránh 2 đơn cùng giữ 1 khung giờ.
     */
    private static void requireNotFinal(Booking booking) {
        if (booking.getStatus() == BookingStatus.tu_choi) {
            throw new BadRequestException("Đơn đã bị từ chối, không thể đổi trạng thái");
        }
        if (booking.getStatus() == BookingStatus.da_huy) {
            throw new BadRequestException("Đơn đã bị hủy, không thể đổi trạng thái");
        }
    }

    private void lockPitch(Long pitchId) {
        pitchLockRepository.lockByPitchId(pitchId)
                .orElseThrow(() -> new IllegalStateException("Thiếu dòng khóa cho sân " + pitchId));
    }
}
