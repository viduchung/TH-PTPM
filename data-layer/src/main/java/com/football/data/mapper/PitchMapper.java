package com.football.data.mapper;

import com.football.data.dto.pitch.GoldenHourPriceDTO;
import com.football.data.dto.pitch.PitchResponseDTO;
import com.football.data.dto.pitch.PriceResponseDTO;
import com.football.data.pitch.entity.GoldenHourPrice;
import com.football.data.pitch.entity.Pitch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper an toàn chuyển đổi giữa Entity Pitch và các DTO PitchResponseDTO, PriceResponseDTO
 * 
 * NGUYÊN TẮC:
 * - Tránh lỗi LazyInitializationException bằng cách kiểm tra và map tường minh danh sách khung giá giờ vàng.
 * - Cung cấp phương thức toPriceResponseDTO trả về bản chụp giá gọn nhẹ phục vụ Service Consumer (Booking Service).
 */
public final class PitchMapper {

    private PitchMapper() {
        // Utility class
    }

    public static PitchResponseDTO toDTO(Pitch pitch) {
        if (pitch == null) {
            return null;
        }

        List<GoldenHourPriceDTO> ghpDtos = new ArrayList<>();
        if (pitch.getGoldenHourPrices() != null) {
            ghpDtos = pitch.getGoldenHourPrices().stream()
                    .map(PitchMapper::toGoldenHourPriceDTO)
                    .collect(Collectors.toList());
        }

        return PitchResponseDTO.builder()
                .id(pitch.getId())
                .name(pitch.getName())
                .type(pitch.getType())
                .pricePerHour(pitch.getPricePerHour())
                .address(pitch.getAddress())
                .status(pitch.getStatus())
                .description(pitch.getDescription())
                .available("AVAILABLE".equalsIgnoreCase(pitch.getStatus()))
                .goldenHourPrices(ghpDtos)
                .build();
    }

    public static GoldenHourPriceDTO toGoldenHourPriceDTO(GoldenHourPrice ghp) {
        if (ghp == null) {
            return null;
        }

        return GoldenHourPriceDTO.builder()
                .id(ghp.getId())
                .dayOfWeek(ghp.getDayOfWeek())
                .startTime(ghp.getStartTime())
                .endTime(ghp.getEndTime())
                .pricePerHour(ghp.getPricePerHour())
                .description(ghp.getDescription())
                .build();
    }

    public static PriceResponseDTO toPriceResponseDTO(Pitch pitch) {
        if (pitch == null) {
            return null;
        }

        return PriceResponseDTO.builder()
                .pitchId(pitch.getId())
                .pitchName(pitch.getName())
                .pitchType(pitch.getType())
                .pricePerHour(pitch.getPricePerHour())
                .currency("VND")
                .status(pitch.getStatus())
                .available("AVAILABLE".equalsIgnoreCase(pitch.getStatus()))
                .build();
    }
}
