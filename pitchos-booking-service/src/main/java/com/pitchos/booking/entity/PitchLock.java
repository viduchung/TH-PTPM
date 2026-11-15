package com.pitchos.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Mỗi sân một dòng khóa trong booking_db. Đơn đặt / sửa đơn của cùng một sân phải giữ khóa dòng này
 * (SELECT ... FOR UPDATE) trong lúc "kiểm tra trùng lịch → lưu", nên đúng cả khi chạy NHIỀU instance
 * Booking Service (thay cho khóa synchronized trong bộ nhớ chỉ đúng với 1 instance).
 * pitch_id chỉ là logical ID, không có Foreign Key sang pitchos_pitch_db.
 */
@Entity
@Table(name = "pitch_locks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PitchLock {

    @Id
    @Column(name = "pitch_id")
    private Long pitchId;
}
