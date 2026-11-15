package com.pitchos.booking.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pitch Service giả lập cho TEST: một HTTP server thật (JDK) trả GET /api/v1/pitches/{id}
 * theo đúng hợp đồng JSON của Pitch Service (id, name, pitchType, pricePerHour, address, status...).
 * Có thể lập trình phản hồi lỗi (404, 500, 503), độ trễ (timeout) và đếm số lần bị gọi.
 */
public final class FakePitchServer implements AutoCloseable {

    public record Reply(int status, String body, long delayMs) {
    }

    private final HttpServer server;
    private final String name;
    private final Map<Long, Reply> defaults = new ConcurrentHashMap<>();
    private final Map<Long, Queue<Reply>> scripted = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> hitsByPitch = new ConcurrentHashMap<>();
    private final AtomicInteger totalHits = new AtomicInteger();

    private FakePitchServer(String name) throws IOException {
        this.name = name;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/api/v1/pitches/", this::handle);
        this.server.setExecutor(Executors.newCachedThreadPool());
        this.server.start();
        reset();
    }

    public static FakePitchServer start(String name) {
        try {
            return new FakePitchServer(name);
        } catch (IOException ex) {
            throw new IllegalStateException("Không khởi động được Pitch Service giả lập", ex);
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Dữ liệu mặc định giống DataInitializer của Pitch Service: 1, 2 hoạt động; 3 bảo trì; id khác → 404. */
    public void reset() {
        defaults.clear();
        scripted.clear();
        hitsByPitch.clear();
        totalHits.set(0);
        defaults.put(1L, ok(pitchJson(1, "300000.00", "hoat_dong", null)));
        defaults.put(2L, ok(pitchJson(2, "500000.00", "hoat_dong", null)));
        defaults.put(3L, ok(pitchJson(3, "250000.00", "bao_tri", null)));
    }

    public void setDefault(long pitchId, Reply reply) {
        defaults.put(pitchId, reply);
    }

    /** Các phản hồi dùng một lần, trả lần lượt trước khi quay về phản hồi mặc định. */
    public void enqueue(long pitchId, Reply... replies) {
        Queue<Reply> queue = scripted.computeIfAbsent(pitchId, id -> new ConcurrentLinkedQueue<>());
        for (Reply reply : replies) {
            queue.add(reply);
        }
    }

    public int hits() {
        return totalHits.get();
    }

    public int hits(long pitchId) {
        AtomicInteger counter = hitsByPitch.get(pitchId);
        return counter == null ? 0 : counter.get();
    }

    public static Reply ok(String body) {
        return new Reply(200, body, 0);
    }

    public static Reply status(int status) {
        return new Reply(status, "{\"status\":" + status + ",\"message\":\"loi gia lap\"}", 0);
    }

    public static Reply delayed(String body, long delayMs) {
        return new Reply(200, body, delayMs);
    }

    public static String pitchJson(long id, String price, String status, Long ownerId) {
        String priceJson = price == null ? "null" : price;
        String owner = ownerId == null ? "" : ",\"ownerId\":" + ownerId;
        return "{\"id\":" + id + ",\"name\":\"San " + id + "\",\"pitchType\":\"san_5\",\"pricePerHour\":" + priceJson
                + ",\"address\":\"Ha Noi\",\"status\":\"" + status + "\",\"createdAt\":\"2026-10-01T08:00:00\"" + owner + "}";
    }

    private void handle(HttpExchange exchange) throws IOException {
        totalHits.incrementAndGet();
        try (exchange) {
            Long pitchId = parseId(exchange.getRequestURI().getPath());
            if (pitchId == null || !"GET".equals(exchange.getRequestMethod())) {
                send(exchange, status(404));
                return;
            }
            hitsByPitch.computeIfAbsent(pitchId, id -> new AtomicInteger()).incrementAndGet();
            Queue<Reply> queue = scripted.get(pitchId);
            Reply reply = queue != null && !queue.isEmpty() ? queue.poll() : defaults.getOrDefault(pitchId, status(404));
            sleep(reply.delayMs());
            send(exchange, reply);
        }
    }

    private static Long parseId(String path) {
        try {
            return Long.parseLong(path.substring("/api/v1/pitches/".length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void send(HttpExchange exchange, Reply reply) throws IOException {
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("X-Pitch-Instance", name);
        exchange.sendResponseHeaders(reply.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        } catch (IOException ex) {
            // Client đã ngắt kết nối (ví dụ do timeout) - bình thường trong test timeout
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
