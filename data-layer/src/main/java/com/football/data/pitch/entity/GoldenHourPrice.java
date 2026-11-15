package com.football.data.pitch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Entity Khung giá giờ vàng (GoldenHourPrice) - Quản lý trong `pitch_db`
 * Ánh xạ tới bảng `golden_hour_prices`.
 * Quan hệ N-1 nội bộ dịch vụ với Sân bóng (Pitch).
 * 
 * Thiết kế: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 */
@Entity
@Table(
        name = "golden_hour_prices",
        indexes = {
                @Index(name = "idx_ghp_pitch_id", columnList = "pitch_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldenHourPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private Pitch pitch;

    @Column(name = "day_of_week", nullable = false, length = 50)
    private String dayOfWeek; // ALL_DAYS, WEEKDAY, WEEKEND

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(length = 255)
    private String description;

    public GoldenHourPrice(Pitch pitch, String dayOfWeek, LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour, String description) {
        this.pitch = pitch;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricePerHour = pricePerHour;
        this.description = description;
    }
}
