package com.football.bookingservice.support;

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
 * HTTP server thật (JDK) đóng vai một service phụ thuộc trong test. Lớp con quyết định phản hồi mặc định;
 * test có thể xếp hàng phản hồi dùng một lần (lỗi 503, trễ...) theo "route key" và đếm số lần bị gọi.
 */
public abstract class FakeHttpService implements AutoCloseable {

    public record Reply(int status, String body, long delayMs) {
        public static Reply ok(String body) {
            return new Reply(200, body, 0);
        }

        public static Reply status(int status) {
            return new Reply(status, "{\"status\":" + status + ",\"message\":\"loi gia lap " + status + "\"}", 0);
        }

        public static Reply delayed(String body, long delayMs) {
            return new Reply(200, body, delayMs);
        }
    }

    private final HttpServer server;
    private final String instanceName;
    private final Map<String, Queue<Reply>> scripted = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final Map<String, String> lastHeaders = new ConcurrentHashMap<>();

    protected FakeHttpService(String instanceName, String contextPath) {
        this.instanceName = instanceName;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException("Không mở được cổng cho service giả lập", ex);
        }
        server.createContext(contextPath, this::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    /** Trả route key (vd "pitch:1", "quote:1") và phản hồi mặc định cho request. */
    protected abstract Route route(HttpExchange exchange);

    protected record Route(String key, Reply defaultReply) {
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void enqueue(String routeKey, Reply... replies) {
        Queue<Reply> queue = scripted.computeIfAbsent(routeKey, key -> new ConcurrentLinkedQueue<>());
        for (Reply reply : replies) {
            queue.add(reply);
        }
    }

    public int hits(String routeKey) {
        AtomicInteger counter = hits.get(routeKey);
        return counter == null ? 0 : counter.get();
    }

    public int totalHits() {
        return hits.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public String lastHeader(String name) {
        return lastHeaders.get(name.toLowerCase());
    }

    public void resetScripts() {
        scripted.clear();
        hits.clear();
        lastHeaders.clear();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            exchange.getRequestHeaders().forEach((name, values) ->
                    lastHeaders.put(name.toLowerCase(), String.join(",", values)));
            Route route = route(exchange);
            hits.computeIfAbsent(route.key(), key -> new AtomicInteger()).incrementAndGet();
            Queue<Reply> queue = scripted.get(route.key());
            Reply reply = queue != null && !queue.isEmpty() ? queue.poll() : route.defaultReply();
            sleep(reply.delayMs());
            send(exchange, reply);
        }
    }

    private void send(HttpExchange exchange, Reply reply) throws IOException {
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("X-Instance", instanceName);
        exchange.sendResponseHeaders(reply.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        } catch (IOException clientGone) {
            // Client đã ngắt (do timeout) - bình thường trong test timeout
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

    @Override
    public void close() {
        server.stop(0);
    }
}
