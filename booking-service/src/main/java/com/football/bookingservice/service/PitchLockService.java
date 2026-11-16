package com.football.bookingservice.service;

import com.football.bookingservice.entity.PitchLock;
import com.football.bookingservice.repository.PitchLockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Bảo đảm sân đã có dòng trong pitch_locks trước khi BookingWriter khóa nó (SELECT ... FOR UPDATE).
 * CỐ Ý không có @Transactional và gọi NGOÀI transaction đặt sân: mỗi lệnh repository chạy transaction riêng,
 * hai request cùng tạo dòng khóa thì request chậm hơn nhận lỗi trùng khóa chính, bỏ qua là đủ.
 */
@Slf4j
@Service
public class PitchLockService {

    private final PitchLockRepository pitchLockRepository;

    public PitchLockService(PitchLockRepository pitchLockRepository) {
        this.pitchLockRepository = pitchLockRepository;
    }

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
