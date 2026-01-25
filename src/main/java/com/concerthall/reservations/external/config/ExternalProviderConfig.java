package com.concerthall.reservations.external.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class ExternalProviderConfig {

    private final ExternalProviderProperties properties;

    @Bean(name = "externalProviderRestTemplate")
    public RestTemplate externalProviderRestTemplate() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(properties.getTimeout()))
                .setReadTimeout(Duration.ofSeconds(properties.getTimeout()))
                .requestFactory(() -> new BufferingClientHttpRequestFactory(
                        new SimpleClientHttpRequestFactory()
                ))
                .errorHandler(new ExternalProviderErrorHandler(objectMapper))
                .interceptors(new ApiKeyInterceptor(properties.getApiKey()))
                .build();
    }
}
