package com.football.bookingservice.support;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * auth-service giả lập: GET /api/v1/auth/me trả UserResponse (id, username, role, active...) theo đúng
 * hợp đồng của AuthController (Hoàng). Token nào chưa đăng ký -> 401.
 */
public final class FakeAuthService extends FakeHttpService {

    public static final String ME_KEY = "me";
    private final Map<String, String> usersByToken = new ConcurrentHashMap<>();

    public FakeAuthService() {
        super("auth-main", "/api/v1/auth/");
    }

    public void register(String token, long id, String username, String role, boolean active) {
        usersByToken.put(token, "{\"id\":" + id + ",\"username\":\"" + username + "\",\"role\":\"" + role
                + "\",\"fullName\":\"Test\",\"phone\":\"0900000000\",\"active\":" + active
                + ",\"createdAt\":\"2026-10-01T08:00:00\"}");
    }

    @Override
    protected Route route(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
        String body = usersByToken.get(token);
        return new Route(ME_KEY, body == null ? Reply.status(401) : Reply.ok(body));
    }
}
