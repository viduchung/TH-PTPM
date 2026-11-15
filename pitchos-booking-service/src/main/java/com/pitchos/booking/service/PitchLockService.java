package com.pitchos.booking.service;

import com.pitchos.booking.entity.PitchLock;
import com.pitchos.booking.repository.PitchLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Bảo đảm sân đã có dòng khóa trong bảng pitch_locks trước khi BookingWriter khóa nó (SELECT ... FOR UPDATE).
 * CỐ Ý không có @Transactional và phải gọi NGOÀI transaction đặt sân: mỗi lệnh repository chạy transaction riêng,
 * nên nếu 2 request cùng tạo dòng khóa thì request chậm hơn nhận lỗi trùng khóa chính, bỏ qua là đủ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PitchLockService {

    private final PitchLockRepository pitchLockRepository;

    public void ensureLockRow(Long pitchId) {
        if (pitchLockRepository.existsById(pitchId)) {
            return;
        }
        try {
            pitchLockRepository.saveAndFlush(new PitchLock(pitchId));
        } catch (DataIntegrityViolationException ex) {
            log.debug("Dòng khóa của sân {} vừa được request khác tạo", pitchId);
        }
    }
}
