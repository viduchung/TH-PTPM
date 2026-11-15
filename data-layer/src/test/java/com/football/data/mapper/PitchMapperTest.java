package com.football.data.mapper;

import com.football.data.dto.pitch.PitchResponseDTO;
import com.football.data.dto.pitch.PriceResponseDTO;
import com.football.data.pitch.entity.GoldenHourPrice;
import com.football.data.pitch.entity.Pitch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

public class PitchMapperTest {

    @Test
    @DisplayName("PitchMapper: Chuyển đổi Entity sang PitchResponseDTO kèm danh sách giờ vàng")
    void testPitchToDTO() {
        Pitch pitch = Pitch.builder()
                .id(1L)
                .name("Sân 5 số 1 Bách Khoa")
                .type("SAN_5")
                .pricePerHour(new BigDecimal("250000.00"))
                .address("Hà Nội")
                .status("AVAILABLE")
                .description("Mặt cỏ mới")
                .build();

        GoldenHourPrice ghp = GoldenHourPrice.builder()
                .id(101L)
                .dayOfWeek("WEEKDAY")
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(20, 30))
                .pricePerHour(new BigDecimal("320000.00"))
                .description("Giờ cao điểm")
                .build();

        pitch.addGoldenHourPrice(ghp);

        PitchResponseDTO dto = PitchMapper.toDTO(pitch);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Sân 5 số 1 Bách Khoa");
        assertThat(dto.isAvailable()).isTrue();
        assertThat(dto.getGoldenHourPrices()).hasSize(1);
        assertThat(dto.getGoldenHourPrices().get(0).getPricePerHour()).isEqualByComparingTo("320000.00");
    }

    @Test
    @DisplayName("PitchMapper: Chuyển đổi sang PriceResponseDTO tinh gọn cho Booking Service")
    void testToPriceResponseDTO() {
        Pitch pitch = Pitch.builder()
                .id(2L)
                .name("Sân 7 A Cầu Giấy")
                .type("SAN_7")
                .pricePerHour(new BigDecimal("450000.00"))
                .status("AVAILABLE")
                .build();

        PriceResponseDTO priceDto = PitchMapper.toPriceResponseDTO(pitch);

        assertThat(priceDto).isNotNull();
        assertThat(priceDto.getPitchId()).isEqualTo(2L);
        assertThat(priceDto.getPitchName()).isEqualTo("Sân 7 A Cầu Giấy");
        assertThat(priceDto.getPricePerHour()).isEqualByComparingTo("450000.00");
        assertThat(priceDto.isAvailable()).isTrue();
        assertThat(priceDto.getCurrency()).isEqualTo("VND");
    }
}

