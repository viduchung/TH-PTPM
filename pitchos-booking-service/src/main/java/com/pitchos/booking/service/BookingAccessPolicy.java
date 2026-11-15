package com.pitchos.booking.service;

import com.pitchos.booking.client.PitchGateway;
import com.pitchos.booking.dto.PitchResponseDTO;
import com.pitchos.booking.entity.Booking;
import com.pitchos.booking.exception.ForbiddenException;
import com.pitchos.booking.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TH5 - Quyền sở hữu dữ liệu:
 *   user   : chỉ xem / sửa / hủy đơn của chính mình (userId lấy từ JWT).
 *   chusan : chỉ thao tác đơn thuộc sân mình quản lý. Pitch Service HIỆN CHƯA trả ownerId nên:
 *            - nếu Pitch trả ownerId → so sánh với userId trong JWT (403 nếu khác);
 *            - nếu chưa có ownerId   → theo booking.security.chusan-without-owner-info
 *              (true = cho phép như hệ thống cũ và ghi cảnh báo; false = chặn 403).
 *   admin  : toàn quyền quản trị.
 */
@Slf4j
@Component
public class BookingAccessPolicy {

    static final String NOT_YOUR_BOOKING = "Bạn không có quyền truy cập đơn đặt sân của người khác";
    static final String NOT_YOUR_PITCH = "Bạn không phải chủ của sân trong đơn đặt này";
    static final String OWNER_UNVERIFIED =
            "Chưa xác minh được chủ sân (Pitch Service chưa cung cấp ownerId) nên không cho phép thao tác";

    private final PitchGateway pitchGateway;
    private final boolean allowOwnerWithoutOwnerInfo;

    public BookingAccessPolicy(PitchGateway pitchGateway,
                               @Value("${booking.security.chusan-without-owner-info:true}") boolean allowOwnerWithoutOwnerInfo) {
        this.pitchGateway = pitchGateway;
        this.allowOwnerWithoutOwnerInfo = allowOwnerWithoutOwnerInfo;
    }

    public void requireCanView(CurrentUser user, Booking booking) {
        if (user.isAdmin()) {
            return;
        }
        if (user.isOwner()) {
            requireOwnsPitch(user, booking.getPitchId());
            return;
        }
        requireBookingOwner(user, booking);
    }

    public void requireBookingOwner(CurrentUser user, Booking booking) {
        if (!booking.getUserId().equals(user.userId())) {
            throw new ForbiddenException(NOT_YOUR_BOOKING);
        }
    }

    /** Gọi Pitch Service để lấy ownerId của sân (ngoài transaction). Pitch sập → 503, không cho qua. */
    public void requireOwnsPitch(CurrentUser owner, Long pitchId) {
        PitchResponseDTO pitch = pitchGateway.getPitch(pitchId);
        if (pitch.getOwnerId() != null) {
            if (!pitch.getOwnerId().equals(owner.userId())) {
                throw new ForbiddenException(NOT_YOUR_PITCH);
            }
            return;
        }
        if (!allowOwnerWithoutOwnerInfo) {
            throw new ForbiddenException(OWNER_UNVERIFIED);
        }
        log.warn("Pitch Service chưa trả ownerId cho sân {}: cho phép chủ sân {} theo cấu hình tương thích",
                pitchId, owner.userId());
    }

    /** Chủ sân xem danh sách tất cả đơn chỉ khi chưa thể lọc theo sân sở hữu và cấu hình cho phép. */
    public void requireOwnerMayListAll() {
        if (!allowOwnerWithoutOwnerInfo) {
            throw new ForbiddenException(OWNER_UNVERIFIED);
        }
    }
}
