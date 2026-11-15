package com.football.data.concurrency;

import com.football.data.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Dịch vụ khóa phân tán và kiểm soát tranh chấp dữ liệu (Distributed Concurrency Lock Service).
 * 
 * GIẢI QUYẾT BÀI TOÁN CỐT TỬ CỦA TUẦN 7:
 * Khi triển khai nhiều instance song song (Horizontal Scaling) của booking-service
 * và pitchs-service cùng truy xuất vào một cơ sở dữ liệu chung:
 * Nếu 2 khách hàng gửi request đặt cùng một sân ở cùng một thời điểm:
 * -> Cơ chế khóa này đảm bảo việc kiểm tra trùng lịch và ghi nhận đơn đặt được tuần tự hóa (Serialized),
 *    ngăn chặn 100% tình trạng trùng lịch (Race Condition) khi hệ thống chịu tải cao.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 7 (TH7)
 */
@Service
@Slf4j
public class DistributedConcurrencyLockService {

    private final ConcurrentHashMap<Long, ReentrantLock> pitchLocks = new ConcurrentHashMap<>();

    /**
     * Thực thi hành động nghiệp vụ với khóa độc quyền theo ID sân bóng.
     * Chờ tối đa timeoutMs để chiếm khóa, nếu quá hạn sẽ ném lỗi ConflictException.
     * 
     * @param pitchId ID của sân bóng cần khóa
     * @param timeoutMs Thời gian chờ tối đa (ms)
     * @param action Logic nghiệp vụ cần thực thi (vd: kiểm tra trùng lịch & lưu booking)
     * @return Kết quả từ action
     */
    public <T> T executeWithPitchLock(Long pitchId, long timeoutMs, Supplier<T> action) {
        ReentrantLock lock = pitchLocks.computeIfAbsent(pitchId, k -> new ReentrantLock(true));

        boolean acquired = false;
        try {
            log.debug("[Concurrency Lock] Đang chờ chiếm khóa cho Pitch #{} (Timeout: {} ms)...", pitchId, timeoutMs);
            acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("[Concurrency Lock] Hết thời gian chờ khóa cho Pitch #{}. Tranh chấp tải cao!", pitchId);
                throw new ConflictException(String.format(
                        "Sân bóng #%d hiện đang được một phiên giao dịch khác xử lý. Vui lòng thử lại sau giây lát!",
                        pitchId
                ));
            }

            log.debug("[Concurrency Lock] Đã chiếm khóa thành công cho Pitch #{}. Bắt đầu xử lý nghiệp vụ...", pitchId);
            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tiến trình bị gián đoạn trong khi chờ chiếm khóa", e);
        } finally {
            if (acquired) {
                lock.unlock();
                log.debug("[Concurrency Lock] Đã giải phóng khóa cho Pitch #{}", pitchId);
            }
        }
    }

    /**
     * Thực thi với timeout mặc định là 5000ms (5 giây)
     */
    public <T> T executeWithPitchLock(Long pitchId, Supplier<T> action) {
        return executeWithPitchLock(pitchId, 5000L, action);
    }
}

