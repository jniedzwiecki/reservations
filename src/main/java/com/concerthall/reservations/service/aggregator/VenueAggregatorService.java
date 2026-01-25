package com.concerthall.reservations.service.aggregator;

import com.concerthall.reservations.domain.Venue;
import com.concerthall.reservations.domain.enums.VenueSource;
import com.concerthall.reservations.dto.response.VenueResponse;
import com.concerthall.reservations.external.adapter.ExternalVenueAdapter;
import com.concerthall.reservations.external.client.ExternalVenueProviderClient;
import com.concerthall.reservations.external.exception.ExternalProviderException;
import com.concerthall.reservations.external.model.ExternalVenueResponse;
import com.concerthall.reservations.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class VenueAggregatorService {

    private final VenueRepository venueRepository;
    private final ExternalVenueProviderClient externalClient;
    private final ExternalVenueAdapter venueAdapter;

    /**
     * Get all venues from both internal and external sources
     */
    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        final List<VenueResponse> internalVenues = getInternalVenues();
        final List<VenueResponse> externalVenues = getExternalVenues();

        return mergeVenues(internalVenues, externalVenues);
    }

    /**
     * Get venue by ID - check both internal and external sources
     * For external venues, the UUID is deterministic based on external ID
     */
    @Transactional(readOnly = true)
    public VenueResponse getVenueById(UUID id) {
        // First try internal database
        final Optional<Venue> internalVenue = venueRepository.findById(id);
        if (internalVenue.isPresent()) {
            return toResponse(internalVenue.get());
        }

        // If not found internally, it might be an external venue
        // We can't efficiently look up external venues by UUID without caching
        // For now, return null - this will be improved with caching in future
        log.debug("Venue {} not found in internal database", id);
        return null;
    }

    /**
     * Get venue by external ID
     */
    public VenueResponse getVenueByExternalId(String externalId) {
        // First check if we have it cached in database
        final Optional<Venue> cachedVenue = venueRepository.findByExternalId(externalId);
        if (cachedVenue.isPresent()) {
            return toResponse(cachedVenue.get());
        }

        // Fetch from external API
        try {
            final ExternalVenueResponse externalVenue = externalClient.getVenueById(externalId);
            return venueAdapter.toVenueResponse(externalVenue);
        } catch (ExternalProviderException e) {
            log.error("Failed to fetch external venue {}", externalId, e);
            return null;
        }
    }

    /**
     * Fetch internal venues from database
     */
    private List<VenueResponse> getInternalVenues() {
        return venueRepository.findAll().stream()
                .filter(v -> v.getSource() == VenueSource.INTERNAL)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetch external venues from external API
     * Returns empty list if external API fails (graceful degradation)
     */
    private List<VenueResponse> getExternalVenues() {
        try {
            final List<ExternalVenueResponse> externalVenues = externalClient.getVenues(null);
            return externalVenues.stream()
                    .map(venueAdapter::toVenueResponse)
                    .collect(Collectors.toList());
        } catch (ExternalProviderException e) {
            log.error("Failed to fetch external venues, returning only internal venues", e);
            return Collections.emptyList(); // Graceful degradation
        }
    }

    /**
     * Merge internal and external venues
     * Sort by name for consistent ordering
     */
    private List<VenueResponse> mergeVenues(
            List<VenueResponse> internal,
            List<VenueResponse> external
    ) {
        return Stream.concat(internal.stream(), external.stream())
                .sorted(Comparator.comparing(VenueResponse::getName))
                .collect(Collectors.toList());
    }

    /**
     * Convert internal Venue entity to VenueResponse
     */
    private VenueResponse toResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .description(venue.getDescription())
                .capacity(venue.getCapacity())
                .source(venue.getSource())
                .externalId(venue.getExternalId())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}
