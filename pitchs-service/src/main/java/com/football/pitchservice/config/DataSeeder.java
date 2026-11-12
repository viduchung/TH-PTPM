package com.football.pitchservice.config;

import com.football.pitchservice.entity.Pitch;
import com.football.pitchservice.repository.PitchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final PitchRepository pitchRepository;

    public DataSeeder(PitchRepository pitchRepository) {
        this.pitchRepository = pitchRepository;
    }

    @Override
    public void run(String... args) {
        if (pitchRepository.count() == 0) {
            log.info("Chưa có dữ liệu sân bóng, tiến hành khởi tạo dữ liệu mẫu (Data Seeding)...");

            List<Pitch> samplePitches = List.of(
                    new Pitch(
                            "Sân 5 số 1 - Cỏ nhân tạo",
                            "SAN_5",
                            new BigDecimal("250000.00"),
                            "Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội",
                            "AVAILABLE",
                            "Sân 5 người, mặt cỏ nhân tạo mới thay, có dàn đèn chiếu sáng ban đêm"
                    ),
                    new Pitch(
                            "Sân 7 A - Tiêu chuẩn FIFA",
                            "SAN_7",
                            new BigDecimal("450000.00"),
                            "Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội",
                            "AVAILABLE",
                            "Sân 7 người chất lượng cao, thoát nước tốt khi trời mưa"
                    ),
                    new Pitch(
                            "Sân 7 B - Cỏ nhân tạo",
                            "SAN_7",
                            new BigDecimal("400000.00"),
                            "Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội",
                            "BOOKED",
                            "Sân 7 người, đang có lịch thi đấu giải phong trào"
                    ),
                    new Pitch(
                            "Sân 11 Lớn - Mặt cỏ tự nhiên",
                            "SAN_11",
                            new BigDecimal("900000.00"),
                            "Trung tâm huấn luyện thể thao Mỹ Đình, Nam Từ Liêm, Hà Nội",
                            "MAINTENANCE",
                            "Sân 11 người kích thước chuẩn quốc tế, đang bảo dưỡng mặt cỏ"
                    )
            );

            pitchRepository.saveAll(samplePitches);
            log.info("Khởi tạo thành công {} sân bóng mẫu vào database!", samplePitches.size());
        }
    }
}
