package com.football.data.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.football.data.dto.auth.TokenValidationResponseDTO;
import com.football.data.dto.booking.BookingResponseDTO;
import com.football.data.dto.common.ApiResponse;
import com.football.data.dto.pitch.PriceResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class InterServiceSerializationContractTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Nguyên tắc Tolerant Reader: Bỏ qua trường lạ không xác định khi nhận từ service khác
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("Contract 1: Serialization TokenValidationResponseDTO trao đổi giữa Auth và Booking/Gateway")
    void testTokenValidationContract() throws Exception {
        TokenValidationResponseDTO response = TokenValidationResponseDTO.valid(
                1L, "customer01", "ROLE_CUSTOMER", "Nguyễn Văn An");

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"valid\":true");
        assertThat(json).contains("\"username\":\"customer01\"");

        TokenValidationResponseDTO deserialized = objectMapper.readValue(json, TokenValidationResponseDTO.class);
        assertThat(deserialized.isValid()).isTrue();
        assertThat(deserialized.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Contract 2: Serialization PriceResponseDTO trao đổi giữa Pitch Service và Booking Service")
    void testPriceResponseContract() throws Exception {
        PriceResponseDTO quote = PriceResponseDTO.builder()
                .pitchId(2L)
                .pitchName("Sân 7 A")
                .pitchType("SAN_7")
                .pricePerHour(new BigDecimal("450000.00"))
                .currency("VND")
                .status("AVAILABLE")
                .available(true)
                .build();

        String json = objectMapper.writeValueAsString(quote);
        assertThat(json).contains("\"pitchId\":2");
        assertThat(json).contains("\"pricePerHour\":450000.00");

        PriceResponseDTO deserialized = objectMapper.readValue(json, PriceResponseDTO.class);
        assertThat(deserialized.getPitchId()).isEqualTo(2L);
        assertThat(deserialized.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Contract 3: Serialization ApiResponse envelope bao bọc BookingResponseDTO")
    void testApiResponseEnvelopeContract() throws Exception {
        BookingResponseDTO booking = BookingResponseDTO.builder()
                .id(10L)
                .userId(1L)
                .pitchId(2L)
                .pitchName("Sân 7 A")
                .startTime(LocalDateTime.of(2026, 11, 20, 18, 0))
                .endTime(LocalDateTime.of(2026, 11, 20, 19, 30))
                .durationHours(1.5)
                .totalPrice(new BigDecimal("675000.00"))
                .status("CHO_DUYET")
                .statusDescription("Chờ duyệt")
                .build();

        ApiResponse<BookingResponseDTO> apiResponse = ApiResponse.ok("Tạo đơn thành công", booking);
        String json = objectMapper.writeValueAsString(apiResponse);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"Tạo đơn thành công\"");
        assertThat(json).contains("\"durationHours\":1.5");
    }

    @Test
    @DisplayName("Contract 4: Tolerant Reader - Cho phép nhận thêm trường mở rộng từ service khác mà không sập")
    void testTolerantReaderPattern() throws Exception {
        // Giả lập service khác gửi thêm các trường tương lai: "extraTelemetry", "requestId", "serverZone"
        String externalJson = """
            {
                "pitchId": 5,
                "pitchName": "Sân 11 FIFA",
                "pitchType": "SAN_11",
                "pricePerHour": 900000.00,
                "currency": "VND",
                "status": "AVAILABLE",
                "available": true,
                "futureFieldA": "v1.2",
                "futureFieldB": 9999
            }
            """;

        PriceResponseDTO deserialized = objectMapper.readValue(externalJson, PriceResponseDTO.class);
        assertThat(deserialized.getPitchId()).isEqualTo(5L);
        assertThat(deserialized.getPitchName()).isEqualTo("Sân 11 FIFA");
        assertThat(deserialized.isAvailable()).isTrue();
    }
}

