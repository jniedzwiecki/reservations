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
     * Note: Not readOnly because we create placeholders for external venues
     */
    @Transactional
    public List<VenueResponse> getAllVenues() {
        final List<VenueResponse> internalVenues = getInternalVenues();
        final List<VenueResponse> externalVenues = getExternalVenues();

        return mergeVenues(internalVenues, externalVenues);
    }

    /**
     * Get venue by ID - check both internal and external sources
     * For external venues with externalId, fetch fresh data from external API
     */
    @Transactional(readOnly = true)
    public VenueResponse getVenueById(UUID id) {
        // Look up venue in database (includes both internal and external placeholders)
        final Optional<Venue> venue = venueRepository.findById(id);
        if (venue.isEmpty()) {
            log.debug("Venue {} not found in database", id);
            return null;
        }

        // If it's an external venue (has externalId), fetch fresh data from external API
        if (venue.get().getExternalId() != null) {
            log.debug("Fetching fresh data for external venue {} (externalId: {})",
                     id, venue.get().getExternalId());
            return getVenueByExternalId(venue.get().getExternalId());
        }

        // Internal venue - return from database
        return toResponse(venue.get());
    }

    /**
     * Get venue by external ID
     * Creates placeholder if it doesn't exist
     */
    @Transactional
    public VenueResponse getVenueByExternalId(String externalId) {
        // First check if we have it cached in database
        final Optional<Venue> cachedVenue = venueRepository.findByExternalId(externalId);
        if (cachedVenue.isPresent()) {
            return toResponse(cachedVenue.get());
        }

        // Fetch from external API and create placeholder
        try {
            final ExternalVenueResponse externalVenue = externalClient.getVenueById(externalId);
            final Venue placeholder = findOrCreateVenuePlaceholder(externalVenue);
            return toResponse(placeholder);
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
     * Creates placeholder records for UUID mapping
     * Returns empty list if external API fails (graceful degradation)
     */
    private List<VenueResponse> getExternalVenues() {
        try {
            final List<ExternalVenueResponse> externalVenues = externalClient.getVenues(null);
            return externalVenues.stream()
                    .map(this::findOrCreateVenuePlaceholder)
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (ExternalProviderException e) {
            log.error("Failed to fetch external venues, returning only internal venues", e);
            return Collections.emptyList(); // Graceful degradation
        }
    }

    /**
     * Find existing venue placeholder or create new one for external venue
     * This creates a UUID -> externalId mapping in the database
     */
    private Venue findOrCreateVenuePlaceholder(ExternalVenueResponse externalVenue) {
        log.info("Finding or creating placeholder for external venue: {}", externalVenue.getId());

        // Check if placeholder already exists
        final Optional<Venue> existing = venueRepository.findByExternalId(externalVenue.getId());
        if (existing.isPresent()) {
            log.info("Updating existing placeholder for external venue: {}", externalVenue.getId());
            // Update placeholder with latest data
            final Venue venue = existing.get();
            venue.setName(externalVenue.getName());
            venue.setAddress(externalVenue.getAddress());
            venue.setDescription(externalVenue.getDescription());
            venue.setCapacity(externalVenue.getCapacity());
            final Venue saved = venueRepository.save(venue);
            log.info("Updated venue placeholder: id={}, externalId={}", saved.getId(), saved.getExternalId());
            return saved;
        }

        // Create new placeholder
        log.info("Creating new placeholder for external venue: {}", externalVenue.getId());
        final Venue newVenue = Venue.builder()
                .name(externalVenue.getName())
                .address(externalVenue.getAddress())
                .description(externalVenue.getDescription())
                .capacity(externalVenue.getCapacity())
                .source(VenueSource.EXTERNAL_PROVIDER)
                .externalId(externalVenue.getId())
                .build();

        final Venue saved = venueRepository.save(newVenue);
        log.info("Created venue placeholder: id={}, externalId={}, name={}",
                saved.getId(), saved.getExternalId(), saved.getName());
        return saved;
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
