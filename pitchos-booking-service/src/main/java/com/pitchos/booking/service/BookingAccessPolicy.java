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
 *   chusan : chỉ thao tác đơn thuộc sân mình quản lý, theo nguyên tắc FAIL-CLOSED:
 *            - Pitch trả ownerId trùng userId trong JWT → cho phép; khác → 403;
 *            - Pitch KHÔNG trả ownerId → 403 (mặc định booking.security.chusan-without-owner-info=false).
 *              Chỉ khi bật chế độ tương thích không an toàn (=true, dành cho demo với Pitch cũ) mới cho qua;
 *            - Pitch lỗi / timeout / sân không tồn tại → ngoại lệ 503 / 404, KHÔNG bao giờ cấp quyền;
 *            - danh sách GET /bookings: 403 vì chưa lọc được theo các sân mà chủ sân sở hữu.
 *   admin  : toàn quyền quản trị, không phụ thuộc Pitch Service.
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
                               @Value("${booking.security.chusan-without-owner-info:false}") boolean allowOwnerWithoutOwnerInfo) {
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
        log.warn("CHẾ ĐỘ TƯƠNG THÍCH KHÔNG AN TOÀN: Pitch Service chưa trả ownerId cho sân {}, vẫn cho phép chủ sân {}",
                pitchId, owner.userId());
    }

    /**
     * Chủ sân xem danh sách: Booking chưa biết chủ sân sở hữu những sân nào (Pitch chưa có API đó) nên không lọc được
     * → 403 ở mặc định. Chế độ tương thích không an toàn (=true) mới trả TẤT CẢ đơn.
     */
    public void requireOwnerMayListAll() {
        if (!allowOwnerWithoutOwnerInfo) {
            throw new ForbiddenException(OWNER_UNVERIFIED);
        }
    }
}
