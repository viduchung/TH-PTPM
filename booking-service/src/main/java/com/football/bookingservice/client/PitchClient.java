package com.football.bookingservice.client;

import com.football.bookingservice.client.dto.PitchInfo;
import com.football.bookingservice.client.dto.PitchQuote;
import com.football.bookingservice.exception.ApiException;
import com.football.bookingservice.exception.BadRequestException;
import com.football.bookingservice.exception.ResourceNotFoundException;
import com.football.bookingservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Consumer của pitchs-service (TH2, TH6).
 *  - GET /api/v1/pitches/{id}        : sân có tồn tại / đang bảo dưỡng không.
 *  - GET /api/v1/pitches/{id}/quote  : giá thuê theo phút + giờ vàng (pitchs-service là nguồn giá duy nhất).
 * Phân loại lỗi: 404 -> 404, 400 -> 400 (giữ thông điệp của pitchs-service), còn lại -> 503.
 */
@Slf4j
@Component
public class PitchClient {

    static final String PITCH_PATH = "/api/v1/pitches/{id}";
    static final String QUOTE_PATH = "/api/v1/pitches/{id}/quote?date={date}&startTime={start}&endTime={end}";
    static final String UNAVAILABLE_MESSAGE = "pitchs-service tạm thời không phản hồi, vui lòng thử lại sau";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final WebClient pitchWebClient;
    private final TransientErrorRetry retry;

    public PitchClient(@Qualifier("pitchWebClient") WebClient pitchWebClient, TransientErrorRetry retry) {
        this.pitchWebClient = pitchWebClient;
        this.retry = retry;
    }

    public PitchInfo getPitch(Long pitchId) {
        PitchInfo pitch = call(pitchId, () -> pitchWebClient.get()
                .uri(PITCH_PATH, pitchId)
                .retrieve()
                .bodyToMono(PitchInfo.class)
                .retryWhen(retry.spec("pitchs-service GET pitch " + pitchId))
                .block(retry.maxBlockingTime()));
        if (pitch == null || !Objects.equals(pitch.id(), pitchId) || !isPositive(pitch.pricePerHour())) {
            throw new ServiceUnavailableException("pitchs-service trả dữ liệu sân không hợp lệ");
        }
        return pitch;
    }

    public PitchQuote getQuote(Long pitchId, LocalDate date, LocalTime start, LocalTime end) {
        PitchQuote quote = call(pitchId, () -> pitchWebClient.get()
                .uri(QUOTE_PATH, pitchId, date, start.format(TIME_FORMAT), end.format(TIME_FORMAT))
                .retrieve()
                .bodyToMono(PitchQuote.class)
                .retryWhen(retry.spec("pitchs-service GET quote " + pitchId))
                .block(retry.maxBlockingTime()));
        if (quote == null || quote.totalAmount() == null || quote.totalAmount().signum() < 0
                || !isPositive(quote.basePricePerHour())) {
            throw new ServiceUnavailableException("pitchs-service trả báo giá không hợp lệ");
        }
        return quote;
    }

    private static <T> T call(Long pitchId, Supplier<T> request) {
        try {
            return request.get();
        } catch (WebClientResponseException ex) {
            throw translate(pitchId, ex);
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Không gọi được pitchs-service cho sân {}: {}", pitchId, ex.getClass().getSimpleName());
            throw new ServiceUnavailableException(UNAVAILABLE_MESSAGE, ex);
        }
    }

    private static ApiException translate(Long pitchId, WebClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == HttpStatus.NOT_FOUND) {
            return new ResourceNotFoundException("Không tìm thấy sân bóng id = " + pitchId);
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return new BadRequestException(providerMessage(ex));
        }
        log.warn("pitchs-service trả HTTP {} cho sân {}", ex.getStatusCode().value(), pitchId);
        return new ServiceUnavailableException(UNAVAILABLE_MESSAGE, ex);
    }

    /** Thông điệp 400 của pitchs-service là câu tiếng Việt cho người dùng (vd: "Sân ... đang bảo dưỡng"). */
    private static String providerMessage(WebClientResponseException ex) {
        try {
            Map<?, ?> body = ex.getResponseBodyAs(Map.class);
            if (body != null && body.get("message") instanceof String message && !message.isBlank()) {
                return message;
            }
        } catch (RuntimeException parseError) {
            log.debug("Không đọc được body lỗi của pitchs-service: {}", parseError.getClass().getSimpleName());
        }
        return "pitchs-service từ chối yêu cầu báo giá";
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
