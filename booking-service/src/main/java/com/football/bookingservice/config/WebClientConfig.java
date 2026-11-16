package com.football.bookingservice.config;

import com.football.bookingservice.security.JwtAuthenticationFilter;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient cho từng service phụ thuộc (TH2). Mỗi client có timeout kết nối và timeout chờ phản hồi
 * để pitchs-service treo không kéo booking-service treo theo (chống cascading failure, TH6).
 * Khi load-balanced=true, gắn bộ lọc Spring Cloud LoadBalancer: "http://pitchs-service" được phân giải
 * thành một instance đang đăng ký trên Eureka, xoay vòng round-robin giữa các instance (TH7).
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient pitchWebClient(WebClient.Builder builder, ServiceClientsProperties properties,
                                    ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> loadBalancer,
                                    @Value("${app.internal.service-key}") String internalServiceKey) {
        return build(builder, "pitchs-service", properties.pitch(), properties, loadBalancer)
                .mutate()
                .defaultHeader(JwtAuthenticationFilter.INTERNAL_KEY_HEADER, internalServiceKey)
                .build();
    }

    @Bean
    public WebClient authWebClient(WebClient.Builder builder, ServiceClientsProperties properties,
                                   ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> loadBalancer) {
        return build(builder, "auth-service", properties.auth(), properties, loadBalancer);
    }

    private static WebClient build(WebClient.Builder builder, String name, ServiceClientsProperties.Endpoint endpoint,
                                   ServiceClientsProperties properties,
                                   ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> loadBalancer) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.responseTimeoutMs()));
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(endpoint.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        if (endpoint.loadBalanced()) {
            ReactorLoadBalancerExchangeFilterFunction filter = loadBalancer.getIfAvailable();
            if (filter == null) {
                throw new IllegalStateException("Bật load-balanced cho " + name + " nhưng thiếu Spring Cloud LoadBalancer");
            }
            clientBuilder.filter(filter);
        }
        log.info("booking-service gọi {} tại {} (load-balanced={})", name, endpoint.baseUrl(), endpoint.loadBalanced());
        return clientBuilder.build();
    }
}
