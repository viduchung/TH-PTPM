package com.football.data.pitch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Sân bóng đá (Pitch / Field) - Quản lý trong cơ sở dữ liệu `pitch_db`
 * Ánh xạ tới bảng `pitches`.
 * 
 * Thiết kế: HÙNG (feat/hung-data) - Tuần 1 (TH1)
 */
@Entity
@Table(
        name = "pitches",
        indexes = {
                @Index(name = "idx_pitches_type", columnList = "type"),
                @Index(name = "idx_pitches_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type; // SAN_5, SAN_7, SAN_11

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "AVAILABLE"; // AVAILABLE, BOOKED, MAINTENANCE

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ 1-N nội bộ CSDL pitch_db: 1 Sân bóng có nhiều Khung giá giờ vàng
    @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GoldenHourPrice> goldenHourPrices = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = "AVAILABLE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Pitch(String name, String type, BigDecimal pricePerHour, String address, String status, String description) {
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.goldenHourPrices = new ArrayList<>();
    }

    public void addGoldenHourPrice(GoldenHourPrice price) {
        goldenHourPrices.add(price);
        price.setPitch(this);
    }

    public void removeGoldenHourPrice(GoldenHourPrice price) {
        goldenHourPrices.remove(price);
        price.setPitch(null);
    }
}
