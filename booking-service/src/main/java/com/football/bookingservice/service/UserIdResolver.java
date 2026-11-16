package com.football.bookingservice.service;

import com.football.bookingservice.client.AuthClient;
import com.football.bookingservice.client.dto.AuthUserInfo;
import com.football.bookingservice.exception.ForbiddenException;
import com.football.bookingservice.exception.UnauthorizedException;
import com.football.bookingservice.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Xác định users.id của người đang gọi.
 *  - Token có claim userId (chuẩn data-layer) -> dùng luôn, không gọi mạng.
 *  - Token của auth-service (chỉ có username) -> hỏi auth-service /api/v1/auth/me bằng chính token đó,
 *    rồi nhớ tạm theo username để không gọi lại ở mỗi request.
 */
@Component
public class UserIdResolver {

    static final Duration CACHE_TTL = Duration.ofMinutes(10);
    static final int MAX_CACHE_ENTRIES = 10_000;

    private final AuthClient authClient;
    private final Clock clock;
    private final ConcurrentMap<String, CachedUserId> cache = new ConcurrentHashMap<>();

    public UserIdResolver(AuthClient authClient, Clock clock) {
        this.authClient = authClient;
        this.clock = clock;
    }

    public long requireUserId(AuthenticatedUser user) {
        if (user.userId() != null) {
            return user.userId();
        }
        if (user.bearerToken() == null) {
            throw new ForbiddenException("Lời gọi nội bộ không gắn với người dùng cụ thể");
        }
        Instant now = clock.instant();
        CachedUserId cached = cache.get(user.username());
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.userId();
        }
        AuthUserInfo info = authClient.currentUser(user.bearerToken());
        if (!user.username().equals(info.username())) {
            throw new UnauthorizedException("Thông tin người dùng không khớp với token");
        }
        if (!info.active()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(user.username(), new CachedUserId(info.id(), now.plus(CACHE_TTL)));
        return info.id();
    }

    private record CachedUserId(long userId, Instant expiresAt) {
    }
}
