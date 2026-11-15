package com.pitchos.booking.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitchos.booking.repository.BookingRepository;
import com.pitchos.booking.repository.PitchLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
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
 * Nền chung cho test tích hợp: chạy Booking Service đầy đủ (Spring Boot + Security + JPA trên H2)
 * và gọi Pitch Service giả lập là một HTTP server thật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
// "./application.properties" ở thư mục gốc service (bản cũ) được Spring ưu tiên hơn file profile trong classpath,
// nên nạp cấu hình test bằng @TestPropertySource (độ ưu tiên cao nhất).
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class IntegrationTestBase {

    protected static final FakePitchServer PITCH = FakePitchServer.start("pitch-main");

    protected static final long USER_A = 3L;
    protected static final long USER_B = 4L;
    protected static final long OWNER = 2L;
    protected static final long ADMIN = 1L;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected BookingRepository bookingRepository;
    @Autowired
    protected PitchLockRepository pitchLockRepository;
    @Autowired
    protected ObjectMapper objectMapper;
    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void pitchServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("pitch.service.url", PITCH::baseUrl);
    }

    @BeforeEach
    void resetState() {
        bookingRepository.deleteAll();
        pitchLockRepository.deleteAll();
        PITCH.reset();
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
    protected long createBooking(long userId, long pitchId, LocalDateTime start, LocalDateTime end) throws Exception {
        MvcResult result = postBooking(TestTokens.user(userId), bookingJson(pitchId, start, end)).andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("Tạo đơn thất bại: " + result.getResponse().getContentAsString());
        }
        return json(result).get("id").asLong();
    }

    protected ResultActions getWithToken(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", TestTokens.bearer(token)));
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
