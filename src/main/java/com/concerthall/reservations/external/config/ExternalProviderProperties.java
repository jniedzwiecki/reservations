package com.concerthall.reservations.external.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "external-provider.venue-api")
@Data
public class ExternalProviderProperties {
    private String baseUrl = "https://api.external-venues.com/v1";
    private String apiKey;
    private Boolean enabled = false;
    private Integer timeout = 30; // seconds
    private Retry retry = new Retry();

    @Data
    public static class Retry {
        private Integer maxAttempts = 3;
        private Long backoffDelay = 1000L; // milliseconds
    }
}
