package com.concerthall.reservations.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_provider_config", indexes = {
    @Index(name = "idx_external_provider_config_enabled", columnList = "enabled")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100, name = "provider_name")
    private String providerName;

    @Column(nullable = false, columnDefinition = "TEXT", name = "api_base_url")
    private String apiBaseUrl;

    @Column(nullable = false, columnDefinition = "TEXT", name = "api_key")
    private String apiKey;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "rate_limit_per_hour")
    @Builder.Default
    private Integer rateLimitPerHour = 1000;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @Column(name = "retry_attempts")
    @Builder.Default
    private Integer retryAttempts = 3;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
