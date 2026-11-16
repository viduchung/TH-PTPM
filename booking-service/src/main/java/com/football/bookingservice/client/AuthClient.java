package com.football.bookingservice.client;

import com.football.bookingservice.client.dto.AuthUserInfo;
import com.football.bookingservice.exception.ServiceUnavailableException;
import com.football.bookingservice.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * TH2/TH6 - Token propagation: chuyển tiếp chính JWT của người dùng sang auth-service GET /api/v1/auth/me
 * để biết users.id (token của auth-service không chứa userId, còn booking_db lưu user_id kiểu BIGINT).
 */
@Slf4j
@Component
public class AuthClient {

    static final String ME_PATH = "/api/v1/auth/me";
    static final String UNAVAILABLE_MESSAGE = "auth-service tạm thời không phản hồi, chưa xác định được người dùng";

    private final WebClient authWebClient;
    private final TransientErrorRetry retry;

    public AuthClient(@Qualifier("authWebClient") WebClient authWebClient, TransientErrorRetry retry) {
        this.authWebClient = authWebClient;
        this.retry = retry;
    }

    public AuthUserInfo currentUser(String bearerToken) {
        AuthUserInfo user;
        try {
            user = authWebClient.get()
                    .uri(ME_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .bodyToMono(AuthUserInfo.class)
                    .retryWhen(retry.spec("auth-service GET /me"))
                    .block(retry.maxBlockingTime());
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw new UnauthorizedException("auth-service không chấp nhận phiên đăng nhập này");
            }
            log.warn("auth-service trả HTTP {} cho /me", ex.getStatusCode().value());
            throw new ServiceUnavailableException(UNAVAILABLE_MESSAGE, ex);
        } catch (RuntimeException ex) {
            log.warn("Không gọi được auth-service: {}", ex.getClass().getSimpleName());
            throw new ServiceUnavailableException(UNAVAILABLE_MESSAGE, ex);
        }
        if (user == null || user.id() == null || user.id() <= 0) {
            throw new ServiceUnavailableException("auth-service trả thông tin người dùng không hợp lệ");
        }
        return user;
    }
}
