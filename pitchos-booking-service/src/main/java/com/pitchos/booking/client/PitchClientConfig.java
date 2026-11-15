package com.pitchos.booking.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(PitchClientProperties.class)
public class PitchClientConfig {

    /**
     * WebClient gọi Pitch Service, có timeout kết nối và timeout chờ phản hồi (chống Cascading Failure).
     * Khi bật Service Discovery (TH7), gắn bộ lọc Spring Cloud LoadBalancer: "http://pitchos-pitch-service"
     * được phân giải thành một instance thật (Eureka) theo vòng tròn (round-robin).
     */
    @Bean
    @ConditionalOnProperty(name = "pitch.client.type", havingValue = "webclient", matchIfMissing = true)
    public WebClient pitchWebClient(PitchClientProperties properties,
                                    ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> loadBalancerFilter) {
        PitchClientProperties.Client client = properties.client();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, client.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(client.responseTimeoutMs()));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.service().baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (properties.service().discoveryEnabled()) {
            ReactorLoadBalancerExchangeFilterFunction filter = loadBalancerFilter.getIfAvailable();
            if (filter == null) {
                throw new IllegalStateException("pitch.service.discovery-enabled=true nhưng thiếu Spring Cloud LoadBalancer");
            }
            builder.filter(filter);
        }
        log.info("Booking gọi Pitch Service bằng WebClient tại {} (discovery={})",
                properties.service().baseUrl(), properties.service().discoveryEnabled());
        return builder.build();
    }
}
