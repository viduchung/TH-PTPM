package com.football.bookingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Mỗi sân một dòng khóa trong booking_db (TH6/TH7 chống double-booking).
 * Đặt / sửa đơn của cùng một sân phải giữ khóa dòng này (SELECT ... FOR UPDATE) trong suốt bước
 * "kiểm tra trùng lịch -> lưu", nên vẫn đúng khi chạy NHIỀU instance booking-service dùng chung booking_db
 * (khóa synchronized trong bộ nhớ chỉ đúng với một instance).
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
