package com.pitchos.booking.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình gọi Pitch Service (application.properties, tiền tố "pitch").
 *
 * @param service url (gọi trực tiếp) hoặc serviceId (gọi qua Service Discovery khi discoveryEnabled = true)
 * @param client  loại client và các ngưỡng timeout / retry
 */
@ConfigurationProperties(prefix = "pitch")
public record PitchClientProperties(@DefaultValue Service service, @DefaultValue Client client) {

    public record Service(@DefaultValue("http://localhost:8082") String url,
                          @DefaultValue("false") boolean discoveryEnabled,
                          @DefaultValue("pitchos-pitch-service") String serviceId) {

        /** Địa chỉ gốc WebClient dùng: tên dịch vụ (LoadBalancer phân giải) hoặc URL cố định. */
        public String baseUrl() {
            return discoveryEnabled ? "http://" + serviceId : url;
        }
    }

    /**
     * @param maxRetries số lần THỬ LẠI tối đa (không tính lần gọi đầu), chỉ áp dụng cho lỗi tạm thời của GET
     */
    public record Client(@DefaultValue("webclient") String type,
                         @DefaultValue("2000") int connectTimeoutMs,
                         @DefaultValue("3000") int responseTimeoutMs,
                         @DefaultValue("2") int maxRetries,
                         @DefaultValue("200") long retryBackoffMs) {
    }
}
