package com.pitchos.booking.client;

import com.pitchos.booking.dto.PitchResponseDTO;
import com.pitchos.booking.exception.PitchServiceUnavailableException;
import com.pitchos.booking.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;

/**
 * TH2 - Booking gọi Pitch Service bằng WebClient (GET /api/v1/pitches/{id}).
 * TH6 - Retry có giới hạn: chỉ thử lại lời gọi GET (an toàn, không đổi dữ liệu) khi lỗi TẠM THỜI
 *       (mất kết nối, timeout, 502/503/504), backoff tăng dần, không thử lại 404 hay 500.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pitch.client.type", havingValue = "webclient", matchIfMissing = true)
public class WebClientPitchGateway implements PitchGateway {

    static final String PITCH_PATH = "/api/v1/pitches/{id}";
    static final String NOT_FOUND_MESSAGE = "Không tìm thấy sân bóng với pitchId đã gửi";

    private static final Set<HttpStatus> TRANSIENT_STATUSES =
            Set.of(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT);
    /** Dự phòng thêm cho block(): mọi lần thử đã có responseTimeout riêng. */
    private static final long BLOCK_SAFETY_MARGIN_MS = 1000;

    private final WebClient pitchWebClient;
    private final PitchClientProperties.Client config;

    public WebClientPitchGateway(WebClient pitchWebClient, PitchClientProperties properties) {
        this.pitchWebClient = pitchWebClient;
        this.config = properties.client();
    }

    @Override
    public PitchResponseDTO getPitch(Long pitchId) {
        try {
            PitchResponseDTO pitch = pitchWebClient.get()
                    .uri(PITCH_PATH, pitchId)
                    .retrieve()
                    .bodyToMono(PitchResponseDTO.class)
                    .retryWhen(retrySpec(pitchId))
                    .block(maxBlockingTime());
            return PitchResponseValidator.requireValid(pitchId, pitch);
        } catch (WebClientResponseException.NotFound ex) {
            throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
        } catch (PitchServiceUnavailableException ex) {
            log.warn("Pitch Service trả dữ liệu sân {} không hợp lệ", pitchId);
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Không gọi được Pitch Service cho sân {}: {}", pitchId, ex.toString());
            throw new PitchServiceUnavailableException(PitchServiceUnavailableException.DEFAULT_MESSAGE, ex);
        }
    }

    private Retry retrySpec(Long pitchId) {
        return Retry.backoff(config.maxRetries(), Duration.ofMillis(config.retryBackoffMs()))
                .filter(WebClientPitchGateway::isTransient)
                .doBeforeRetry(signal -> log.info("Thử lại lần {} khi gọi Pitch Service cho sân {}: {}",
                        signal.totalRetries() + 1, pitchId, signal.failure().toString()))
                // Hết lượt thử: ném lại lỗi gốc để phân loại 404 / 503 như bình thường
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    static boolean isTransient(Throwable error) {
        if (error instanceof WebClientRequestException) {
            return true; // không kết nối được hoặc quá thời gian chờ phản hồi
        }
        return error instanceof WebClientResponseException response
                && TRANSIENT_STATUSES.contains(HttpStatus.resolve(response.getStatusCode().value()));
    }

    private Duration maxBlockingTime() {
        long perAttempt = (long) config.connectTimeoutMs() + config.responseTimeoutMs();
        long backoffBudget = config.retryBackoffMs() * (1L << Math.max(config.maxRetries(), 0));
        return Duration.ofMillis(perAttempt * (config.maxRetries() + 1L) + backoffBudget + BLOCK_SAFETY_MARGIN_MS);
    }
}
