package com.concerthall.reservations.external.adapter;

import com.concerthall.reservations.domain.enums.VenueSource;
import com.concerthall.reservations.dto.response.VenueResponse;
import com.concerthall.reservations.external.model.ExternalVenueResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class ExternalVenueAdapter {

    private static final String EXTERNAL_VENUE_NAMESPACE = "external-venue";

    /**
     * Convert external venue response to internal venue response
     */
    public VenueResponse toVenueResponse(ExternalVenueResponse external) {
        if (external == null) {
            return null;
        }

        return VenueResponse.builder()
                .id(generateInternalId(external.getId()))
                .name(external.getName())
                .address(formatAddress(external))
                .description(external.getDescription())
                .capacity(external.getCapacity())
                .source(VenueSource.EXTERNAL_PROVIDER)
                .externalId(external.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Generate deterministic UUID from external ID
     * Uses UUID v5 (name-based with SHA-1) for consistent ID generation
     */
    private UUID generateInternalId(String externalId) {
        // Create namespace UUID from constant string
        final UUID namespace = UUID.nameUUIDFromBytes(EXTERNAL_VENUE_NAMESPACE.getBytes());

        // Generate deterministic UUID from external ID within namespace
        return UUID.nameUUIDFromBytes((namespace.toString() + ":" + externalId).getBytes());
    }

    /**
     * Format address from external format to internal format
     * External: address, city, state, country, postalCode
     * Internal: single address string
     */
    private String formatAddress(ExternalVenueResponse external) {
        final StringBuilder addressBuilder = new StringBuilder();

        if (external.getAddress() != null && !external.getAddress().isEmpty()) {
            addressBuilder.append(external.getAddress());
        }

        if (external.getCity() != null && !external.getCity().isEmpty()) {
            if (!addressBuilder.isEmpty()) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(external.getCity());
        }

        if (external.getState() != null && !external.getState().isEmpty()) {
            if (!addressBuilder.isEmpty()) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(external.getState());
        }

        if (external.getPostalCode() != null && !external.getPostalCode().isEmpty()) {
            if (!addressBuilder.isEmpty()) {
                addressBuilder.append(" ");
            }
            addressBuilder.append(external.getPostalCode());
        }

        if (external.getCountry() != null && !external.getCountry().isEmpty()) {
            if (!addressBuilder.isEmpty()) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(external.getCountry());
        }

        return addressBuilder.toString();
    }
}
