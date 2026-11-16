package com.football.bookingservice.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.bookingservice.repository.BookingRepository;
import com.football.bookingservice.repository.IdempotencyRecordRepository;
import com.football.bookingservice.repository.PitchLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Nền chung cho test tích hợp: chạy booking-service đầy đủ (Spring Boot + Security + JPA trên H2),
 * gọi pitchs-service / auth-service giả lập là HTTP server thật.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final FakePitchService PITCH = new FakePitchService("pitch-main");
    protected static final FakeAuthService AUTH = new FakeAuthService();

    protected static final long CUSTOMER_A = 1L;
    protected static final long CUSTOMER_B = 2L;
    protected static final long OWNER = 3L;
    protected static final long ADMIN = 4L;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected BookingRepository bookingRepository;
    @Autowired
    protected PitchLockRepository pitchLockRepository;
    @Autowired
    protected IdempotencyRecordRepository idempotencyRepository;
    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void serviceUrls(DynamicPropertyRegistry registry) {
        registry.add("app.clients.pitch.base-url", PITCH::baseUrl);
        registry.add("app.clients.auth.base-url", AUTH::baseUrl);
    }

    @BeforeEach
    void resetState() {
        bookingRepository.deleteAll();
        pitchLockRepository.deleteAll();
        idempotencyRepository.deleteAll();
        PITCH.resetScripts();
        AUTH.resetScripts();
    }

    /** Khung giờ ở tương lai (ngày mai + dayOffset) để không vướng quy tắc "startTime phải ở tương lai". */
    protected static LocalDateTime at(int dayOffset, int hour, int minute) {
        return LocalDateTime.of(LocalDate.now().plusDays(1L + dayOffset), LocalTime.of(hour, minute));
    }

    protected static String bookingJson(long pitchId, LocalDateTime start, LocalDateTime end) {
        return "{\"pitchId\":" + pitchId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    protected ResultActions postBooking(String token, String json) throws Exception {
        return mvc.perform(post("/api/v1/bookings")
                .header("Authorization", TestTokens.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected ResultActions postBooking(String token, String json, String idempotencyKey) throws Exception {
        return mvc.perform(post("/api/v1/bookings")
                .header("Authorization", TestTokens.bearer(token))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Tạo đơn hợp lệ qua API và trả về id. */
    protected long createBooking(long customerId, long pitchId, LocalDateTime start, LocalDateTime end)
            throws Exception {
        MvcResult result = postBooking(TestTokens.customer(customerId), bookingJson(pitchId, start, end)).andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("Tạo đơn thất bại: " + result.getResponse().getContentAsString());
        }
        return json(result).at("/data/id").asLong();
    }

    protected ResultActions getWithToken(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", TestTokens.bearer(token)));
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
