package com.football.pitchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pitches")
@Data
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
    private String status; // AVAILABLE, BOOKED, MAINTENANCE

    @Column(columnDefinition = "TEXT")
    private String description;

    // Quan hệ One-to-Many: 1 Sân bóng có nhiều Khung giá giờ vàng (TH4)
    @OneToMany(mappedBy = "pitch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GoldenHourPrice> goldenHourPrices = new ArrayList<>();

    // Default no-argument constructor required by JPA
    public Pitch() {
    }

    // Constructor with basic fields
    public Pitch(String name, String type, BigDecimal pricePerHour, String address, String status, String description) {
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.goldenHourPrices = new ArrayList<>();
    }

    public Pitch(Long id, String name, String type, BigDecimal pricePerHour, String address, String status, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.address = address;
        this.status = status;
        this.description = description;
        this.goldenHourPrices = new ArrayList<>();
    }

    // Helper methods quản lý quan hệ 2 chiều
    public void addGoldenHourPrice(GoldenHourPrice price) {
        goldenHourPrices.add(price);
        price.setPitch(this);
    }

    public void removeGoldenHourPrice(GoldenHourPrice price) {
        goldenHourPrices.remove(price);
        price.setPitch(null);
    }

    // Explicit getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GoldenHourPrice> getGoldenHourPrices() {
        return goldenHourPrices;
    }

    public void setGoldenHourPrices(List<GoldenHourPrice> goldenHourPrices) {
        this.goldenHourPrices = goldenHourPrices;
    }
}
