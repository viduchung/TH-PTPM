package com.football.data.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.football.data.dto.integration.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TH6 Unit Test: Hợp đồng tích hợp DTO & Khả năng tương thích ngược (Compatibility)")
public class IntegrationContractCompatibilityTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("TH6 Test 1: PitchAvailabilityResponseDTO serialize/deserialize và chịu lỗi trường lạ (Tolerant Reader)")
    void testPitchAvailabilityResponseDTO_Serialization() throws Exception {
        String jsonWithExtraFields = """
                {
                    "pitchId": 10,
                    "pitchName": "Sân Cầu Giấy 7",
                    "available": true,
                    "pricePerHour": 350000.0,
                    "estimatedTotalPrice": 700000.0,
                    "message": "Sân đang trống",
                    "unexpectedFieldV2": "new feature data",
                    "version": 2
                }
                """;

        PitchAvailabilityResponseDTO dto = objectMapper.readValue(jsonWithExtraFields, PitchAvailabilityResponseDTO.class);
        assertNotNull(dto);
        assertEquals(10L, dto.getPitchId());
        assertEquals("Sân Cầu Giấy 7", dto.getPitchName());
        assertTrue(dto.isAvailable());
        assertEquals(new BigDecimal("350000.0"), dto.getPricePerHour());
    }

    @Test
    @DisplayName("TH6 Test 2: PaymentIntegrationResponseDTO hỗ trợ tuần tự hóa đầy đủ trạng thái")
    void testPaymentIntegrationResponseDTO_Serialization() throws Exception {
        PaymentIntegrationResponseDTO dto = PaymentIntegrationResponseDTO.builder()
                .paymentId(555L)
                .bookingId(101L)
                .amount(new BigDecimal("500000.00"))
                .status("SUCCESS")
                .transactionCode("TXN_123456")
                .success(true)
                .message("Giao dịch thành công qua VNPay")
                .timestamp(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        PaymentIntegrationResponseDTO parsed = objectMapper.readValue(json, PaymentIntegrationResponseDTO.class);

        assertEquals(555L, parsed.getPaymentId());
        assertEquals("TXN_123456", parsed.getTransactionCode());
        assertTrue(parsed.isSuccess());
    }

    @Test
    @DisplayName("TH6 Test 3: SagaCompensationRequest & Response DTO bảo toàn thông tin bồi hoàn")
    void testSagaCompensationDTO_Serialization() throws Exception {
        SagaCompensationRequestDTO req = SagaCompensationRequestDTO.builder()
                .bookingId(202L)
                .reason("Khách hàng hủy trước 24h")
                .refundRequired(true)
                .refundAmount(new BigDecimal("300000.00"))
                .build();

        String reqJson = objectMapper.writeValueAsString(req);
        SagaCompensationRequestDTO parsedReq = objectMapper.readValue(reqJson, SagaCompensationRequestDTO.class);
        assertEquals(202L, parsedReq.getBookingId());
        assertTrue(parsedReq.isRefundRequired());

        SagaCompensationResponseDTO res = SagaCompensationResponseDTO.builder()
                .bookingId(202L)
                .compensated(true)
                .status("REFUNDED")
                .message("Đã hoàn lại 300,000 VNĐ vào ví khách hàng")
                .build();

        String resJson = objectMapper.writeValueAsString(res);
        SagaCompensationResponseDTO parsedRes = objectMapper.readValue(resJson, SagaCompensationResponseDTO.class);
        assertTrue(parsedRes.isCompensated());
        assertEquals("REFUNDED", parsedRes.getStatus());
    }

    @Test
    @DisplayName("TH6 Test 4: BookingIntegrationEventDTO cấu trúc sự kiện truyền tin liên microservice")
    void testBookingIntegrationEventDTO_Serialization() throws Exception {
        BookingIntegrationEventDTO event = BookingIntegrationEventDTO.builder()
                .eventId("evt-001")
                .eventType("BOOKING_CONFIRMED")
                .bookingId(303L)
                .userId(1L)
                .pitchId(5L)
                .amount(new BigDecimal("450000.00"))
                .idempotencyKey("idemp-key-xyz")
                .timestamp(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        BookingIntegrationEventDTO parsed = objectMapper.readValue(json, BookingIntegrationEventDTO.class);

        assertEquals("evt-001", parsed.getEventId());
        assertEquals("BOOKING_CONFIRMED", parsed.getEventType());
        assertEquals(303L, parsed.getBookingId());
    }
}
