package com.football.pitchservice.config;

import com.football.pitchservice.entity.GoldenHourPrice;
import com.football.pitchservice.entity.Pitch;
import com.football.pitchservice.repository.PitchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
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
            log.info("Chưa có dữ liệu sân bóng, tiến hành khởi tạo dữ liệu mẫu kèm giờ vàng (TH3 & TH4)...");

            // Sân 5 số 1
            Pitch pitch1 = new Pitch(
                    "Sân 5 số 1 - Cỏ nhân tạo",
                    "SAN_5",
                    new BigDecimal("250000.00"),
                    "Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội",
                    "AVAILABLE",
                    "Sân 5 người, mặt cỏ nhân tạo mới thay, có dàn đèn chiếu sáng ban đêm"
            );
            pitch1.addGoldenHourPrice(new GoldenHourPrice(
                    "ALL_DAYS",
                    LocalTime.of(17, 30, 0),
                    LocalTime.of(20, 30, 0),
                    new BigDecimal("320000.00"),
                    "Khung giờ cao điểm buổi tối sau giờ hành chính"
            ));

            // Sân 7 A
            Pitch pitch2 = new Pitch(
                    "Sân 7 A - Tiêu chuẩn FIFA",
                    "SAN_7",
                    new BigDecimal("450000.00"),
                    "Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội",
                    "AVAILABLE",
                    "Sân 7 người chất lượng cao, thoát nước tốt khi trời mưa"
            );
            pitch2.addGoldenHourPrice(new GoldenHourPrice(
                    "WEEKDAY",
                    LocalTime.of(17, 0, 0),
                    LocalTime.of(21, 0, 0),
                    new BigDecimal("550000.00"),
                    "Khung giờ vàng các ngày trong tuần (T2 - T6)"
            ));
            pitch2.addGoldenHourPrice(new GoldenHourPrice(
                    "WEEKEND",
                    LocalTime.of(6, 0, 0),
                    LocalTime.of(10, 0, 0),
                    new BigDecimal("500000.00"),
                    "Khung giờ sáng sớm cuối tuần (Thứ 7, CN)"
            ));

            // Sân 7 B
            Pitch pitch3 = new Pitch(
                    "Sân 7 B - Cỏ nhân tạo",
                    "SAN_7",
                    new BigDecimal("400000.00"),
                    "Sân vận động Cầu Giấy, Dịch Vọng, Hà Nội",
                    "BOOKED",
                    "Sân 7 người, đang có lịch thi đấu giải phong trào"
            );

            // Sân 11 Lớn
            Pitch pitch4 = new Pitch(
                    "Sân 11 Lớn - Mặt cỏ tự nhiên",
                    "SAN_11",
                    new BigDecimal("900000.00"),
                    "Trung tâm huấn luyện thể thao Mỹ Đình, Nam Từ Liêm, Hà Nội",
                    "MAINTENANCE",
                    "Sân 11 người kích thước chuẩn quốc tế, đang bảo dưỡng mặt cỏ"
            );

            pitchRepository.saveAll(List.of(pitch1, pitch2, pitch3, pitch4));
            log.info("Khởi tạo thành công 4 sân bóng mẫu và các khung giá giờ vàng vào database!");
        }
    }
}
