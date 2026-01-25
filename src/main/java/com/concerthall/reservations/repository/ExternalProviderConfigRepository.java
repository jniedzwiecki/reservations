package com.concerthall.reservations.repository;

import com.concerthall.reservations.domain.ExternalProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalProviderConfigRepository extends JpaRepository<ExternalProviderConfig, UUID> {

    Optional<ExternalProviderConfig> findByProviderName(String providerName);

    Optional<ExternalProviderConfig> findByProviderNameAndEnabledTrue(String providerName);
}
