package com.football.bookingservice.client;

import com.football.bookingservice.config.ServiceClientsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;

/**
 * TH6: retry có giới hạn, backoff tăng dần + jitter, CHỈ cho lỗi tạm thời (mất kết nối, timeout, 502/503/504).
 * Không retry 4xx (lỗi của chính request) hay 500 (lỗi logic phía provider).
 * Chỉ dùng cho lời gọi GET (đọc, an toàn khi gọi lại).
 */
@Slf4j
@Component
public class TransientErrorRetry {

    private static final Set<HttpStatus> TRANSIENT_STATUSES =
            Set.of(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT);
    /** Dự phòng cho block(): mỗi lần thử đã có responseTimeout riêng. */
    private static final long BLOCK_SAFETY_MARGIN_MS = 1000;

    private final ServiceClientsProperties properties;

    public TransientErrorRetry(ServiceClientsProperties properties) {
        this.properties = properties;
    }

    public Retry spec(String target) {
        return Retry.backoff(properties.maxRetries(), Duration.ofMillis(properties.retryBackoffMs()))
                .filter(TransientErrorRetry::isTransient)
                .doBeforeRetry(signal -> log.info("Thử lại lần {} khi gọi {}: {}",
                        signal.totalRetries() + 1, target, signal.failure().getClass().getSimpleName()))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    /** Thời gian tối đa block() chờ, đủ cho mọi lần thử + backoff. */
    public Duration maxBlockingTime() {
        long perAttempt = (long) properties.connectTimeoutMs() + properties.responseTimeoutMs();
        long backoffBudget = properties.retryBackoffMs() * (1L << Math.max(properties.maxRetries(), 0));
        return Duration.ofMillis(perAttempt * (properties.maxRetries() + 1L) + backoffBudget + BLOCK_SAFETY_MARGIN_MS);
    }

    static boolean isTransient(Throwable error) {
        if (error instanceof WebClientRequestException) {
            return true;
        }
        return error instanceof WebClientResponseException response
                && TRANSIENT_STATUSES.contains(HttpStatus.resolve(response.getStatusCode().value()));
    }
}
