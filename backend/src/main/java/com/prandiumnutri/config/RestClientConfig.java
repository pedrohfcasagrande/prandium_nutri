package com.prandiumnutri.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // Timeouts em segundos. Podem ser override via env vars (ex: em prod aumentar
    // read-timeout se algum provider externo for consistentemente lento).
    @Value("${prandiumnutri.http.connect-timeout-seconds:10}")
    private int connectTimeoutSeconds;

    @Value("${prandiumnutri.http.read-timeout-seconds:30}")
    private int readTimeoutSeconds;

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
