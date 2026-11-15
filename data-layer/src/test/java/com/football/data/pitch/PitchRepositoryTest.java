package com.football.data.pitch;

import com.football.data.pitch.entity.GoldenHourPrice;
import com.football.data.pitch.entity.Pitch;
import com.football.data.pitch.repository.GoldenHourPriceRepository;
import com.football.data.pitch.repository.PitchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class PitchRepositoryTest {

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private GoldenHourPriceRepository goldenHourPriceRepository;

    @Test
    @DisplayName("Lưu và tìm kiếm sân bóng theo loại và trạng thái")
    void testSaveAndFindByTypeAndStatus() {
        Pitch p1 = Pitch.builder()
                .name("Sân 5 Bách Khoa")
                .type("SAN_5")
                .pricePerHour(new BigDecimal("300000.00"))
                .address("Hà Nội")
                .status("AVAILABLE")
                .description("Sân cỏ mới")
                .build();

        Pitch p2 = Pitch.builder()
                .name("Sân 7 Cầu Giấy")
                .type("SAN_7")
                .pricePerHour(new BigDecimal("500000.00"))
                .address("Hà Nội")
                .status("AVAILABLE")
                .description("Sân tiêu chuẩn")
                .build();

        Pitch p3 = Pitch.builder()
                .name("Sân 7 Mỹ Đình")
                .type("SAN_7")
                .pricePerHour(new BigDecimal("550000.00"))
                .address("Hà Nội")
                .status("MAINTENANCE")
                .description("Bảo dưỡng")
                .build();

        pitchRepository.saveAll(List.of(p1, p2, p3));

        List<Pitch> san7List = pitchRepository.findByType("SAN_7");
        assertThat(san7List).hasSize(2);

        List<Pitch> availableList = pitchRepository.findByStatus("AVAILABLE");
        assertThat(availableList).hasSize(2);

        List<Pitch> availableSan7 = pitchRepository.findByTypeAndStatus("SAN_7", "AVAILABLE");
        assertThat(availableSan7).hasSize(1);
        assertThat(availableSan7.get(0).getName()).isEqualTo("Sân 7 Cầu Giấy");
    }

    @Test
    @DisplayName("Tìm kiếm phân trang sân bóng (searchPitches)")
    void testSearchPitchesPageable() {
        pitchRepository.save(new Pitch("Sân 1", "SAN_5", new BigDecimal("200000"), "HN", "AVAILABLE", "desc"));
        pitchRepository.save(new Pitch("Sân 2", "SAN_5", new BigDecimal("250000"), "HN", "AVAILABLE", "desc"));
        pitchRepository.save(new Pitch("Sân 3", "SAN_5", new BigDecimal("300000"), "HN", "BOOKED", "desc"));

        Page<Pitch> page = pitchRepository.searchPitches("SAN_5", "AVAILABLE", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kiểm tra quan hệ 1-N giữa Pitch và GoldenHourPrice nội bộ pitch_db")
    void testPitchAndGoldenHourRelationship() {
        Pitch pitch = Pitch.builder()
                .name("Sân VIP Tây Hồ")
                .type("SAN_7")
                .pricePerHour(new BigDecimal("400000.00"))
                .status("AVAILABLE")
                .build();

        GoldenHourPrice ghp = GoldenHourPrice.builder()
                .dayOfWeek("WEEKDAY")
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(20, 30))
                .pricePerHour(new BigDecimal("550000.00"))
                .description("Giờ vàng buổi tối")
                .build();

        pitch.addGoldenHourPrice(ghp);
        Pitch saved = pitchRepository.save(pitch);

        assertThat(saved.getId()).isNotNull();
        List<GoldenHourPrice> prices = goldenHourPriceRepository.findByPitchId(saved.getId());
        assertThat(prices).hasSize(1);
        assertThat(prices.get(0).getPricePerHour()).isEqualByComparingTo("550000.00");
    }
}
