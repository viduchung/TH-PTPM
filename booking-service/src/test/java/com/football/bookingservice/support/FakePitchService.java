package com.football.bookingservice.support;

import com.sun.net.httpserver.HttpExchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * pitchs-service giả lập theo đúng hợp đồng JSON của PitchController (Linh):
 *   GET /api/v1/pitches/{id}        -> PitchResponseDTO
 *   GET /api/v1/pitches/{id}/quote  -> QuoteResponseDTO (giá = pricePerHour x phút / 60, không giờ vàng)
 * Sân 1: 300.000đ/giờ, sân 2: 500.000đ/giờ, sân 3: MAINTENANCE, id khác -> 404.
 */
public final class FakePitchService extends FakeHttpService {

    private static final String BASE = "/api/v1/pitches/";
    private static final Map<Long, String> PRICES = Map.of(1L, "300000.00", 2L, "500000.00", 3L, "250000.00");

    public FakePitchService(String instanceName) {
        super(instanceName, BASE);
    }

    public static String pitchKey(long pitchId) {
        return "pitch:" + pitchId;
    }

    public static String quoteKey(long pitchId) {
        return "quote:" + pitchId;
    }

    @Override
    protected Route route(HttpExchange exchange) {
        String[] parts = exchange.getRequestURI().getPath().substring(BASE.length()).split("/");
        Long pitchId = parseId(parts[0]);
        if (pitchId == null) {
            return new Route("invalid", Reply.status(404));
        }
        if (parts.length == 2 && "quote".equals(parts[1])) {
            return new Route(quoteKey(pitchId), quoteReply(pitchId, query(exchange)));
        }
        return new Route(pitchKey(pitchId), pitchReply(pitchId));
    }

    private static Reply pitchReply(long pitchId) {
        String price = PRICES.get(pitchId);
        if (price == null) {
            return Reply.status(404);
        }
        String status = pitchId == 3 ? "MAINTENANCE" : "AVAILABLE";
        return Reply.ok("{\"id\":" + pitchId + ",\"name\":\"San " + pitchId + "\",\"type\":\"SAN_7\",\"pricePerHour\":"
                + price + ",\"address\":\"Ha Noi\",\"status\":\"" + status + "\",\"description\":null,\"available\":"
                + "AVAILABLE".equals(status) + ",\"goldenHourPrices\":[]}");
    }

    private static Reply quoteReply(long pitchId, Map<String, String> query) {
        String price = PRICES.get(pitchId);
        if (price == null) {
            return Reply.status(404);
        }
        LocalTime start = LocalTime.parse(query.get("startTime"));
        LocalTime end = LocalTime.parse(query.get("endTime"));
        long minutes = Duration.between(start, end).toMinutes();
        BigDecimal total = new BigDecimal(price).multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return Reply.ok("{\"pitchId\":" + pitchId + ",\"pitchName\":\"San " + pitchId + "\",\"pitchType\":\"SAN_7\","
                + "\"bookingDate\":\"" + query.get("date") + "\",\"startTime\":\"" + start + ":00\",\"endTime\":\""
                + end + ":00\",\"totalMinutes\":" + minutes + ",\"basePricePerHour\":" + price
                + ",\"regularMinutes\":" + minutes + ",\"goldenHourMinutes\":0,\"goldenHourPricePerHour\":null,"
                + "\"totalAmount\":" + total + ",\"goldenHourApplied\":false,\"currency\":\"VND\","
                + "\"calculationBreakdown\":\"" + minutes + " phut x " + price + "/gio\"}");
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String raw = exchange.getRequestURI().getQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                String[] kv = pair.split("=", 2);
                params.put(kv[0], kv.length == 2 ? kv[1] : "");
            }
        }
        return params;
    }

    private static Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
