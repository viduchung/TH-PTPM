package com.football.data.dto.pitch;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO Khung giá giờ vàng
 * Tránh việc Booking Service phải map trực tiếp JPA relationship 1-N phức tạp.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldenHourPriceDTO {

    private Long id;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal pricePerHour;
    private String description;
}

