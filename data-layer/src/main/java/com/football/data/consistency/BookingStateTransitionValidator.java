package com.football.data.consistency;

import com.football.data.booking.entity.BookingStatus;
import com.football.data.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Bộ kiểm soát chuyển đổi trạng thái đơn đặt sân (State Transition Validator).
 * 
 * Đảm bảo tính toàn vẹn và nhất quán dữ liệu trạng thái (State Consistency)
 * giữa các microservice khi cùng tác động lên đơn đặt sân:
 * - CHO_DUYET -> DA_DUYET, TU_CHOI, DA_HUY
 * - DA_DUYET  -> DA_HUY (kèm bồi hoàn/bù trừ)
 * - TU_CHOI   -> Trạng thái kết thúc (Terminal State - không thể thay đổi)
 * - DA_HUY    -> Trạng thái kết thúc (Terminal State - không thể thay đổi)
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Component
public class BookingStateTransitionValidator {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(BookingStatus.class);

    static {
        // Trạng thái chờ duyệt có thể duyệt, từ chối hoặc hủy
        ALLOWED_TRANSITIONS.put(BookingStatus.CHO_DUYET, EnumSet.of(
                BookingStatus.DA_DUYET,
                BookingStatus.TU_CHOI,
                BookingStatus.DA_HUY
        ));

        // Trạng thái đã duyệt chỉ có thể bị hủy nếu có lý do chính đáng / bù trừ
        ALLOWED_TRANSITIONS.put(BookingStatus.DA_DUYET, EnumSet.of(
                BookingStatus.DA_HUY
        ));

        // Các trạng thái kết thúc (Terminal States) không thể đổi sang trạng thái khác
        ALLOWED_TRANSITIONS.put(BookingStatus.TU_CHOI, Collections.emptySet());
        ALLOWED_TRANSITIONS.put(BookingStatus.DA_HUY, Collections.emptySet());
    }

    /**
     * Kiểm tra xem chuyển đổi trạng thái có hợp lệ hay không
     */
    public boolean canTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return true; // Giữ nguyên trạng thái được coi là hợp lệ
        }
        Set<BookingStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        return allowed.contains(targetStatus);
    }

    /**
     * Xác thực chuyển đổi trạng thái, ném ra ConflictException nếu vi phạm ràng buộc quy trình
     */
    public void validateTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new ConflictException(String.format(
                    "Vi phạm tính nhất quán dữ liệu: Không thể chuyển trạng thái đơn đặt sân từ [%s] sang [%s].",
                    currentStatus, targetStatus
            ));
        }
    }

    /**
     * Kiểm tra xem trạng thái đã là trạng thái kết thúc (Terminal) hay chưa
     */
    public boolean isTerminalState(BookingStatus status) {
        return status == BookingStatus.TU_CHOI || status == BookingStatus.DA_HUY;
    }
}
