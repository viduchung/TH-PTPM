package com.football.data.dto.integration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Hợp đồng DTO: Kiểm tra tính khả dụng của sân bóng trước khi tạo đơn đặt sân.
 * Gửi từ booking-service sang pitchs-service.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PitchAvailabilityRequestDTO {

    @NotNull(message = "pitchId không được để trống")
    private Long pitchId;

    @NotNull(message = "startTime không được để trống")
    @Future(message = "Thời gian bắt đầu phải ở tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "endTime không được để trống")
    @Future(message = "Thời gian kết thúc phải ở tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
}

