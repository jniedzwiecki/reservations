package com.concerthall.reservations.external.adapter;

import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.VenueResponse;
import com.concerthall.reservations.external.model.ExternalEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class ExternalEventAdapter {

    private static final String EXTERNAL_EVENT_NAMESPACE = "external-event";

    /**
     * Convert external event response to internal event response
     */
    public EventResponse toEventResponse(
            ExternalEventResponse external,
            VenueResponse venue
    ) {
        if (external == null) {
            return null;
        }

        return EventResponse.builder()
                .id(generateInternalId(external.getId()))
                .name(external.getName())
                .description(external.getDescription())
                .eventDateTime(external.getEventDateTime())
                .capacity(external.getCapacity())
                .price(external.getPrice() != null ? external.getPrice().getAmount() : null)
                .status(mapStatus(external.getStatus()).name())
                .availableTickets(external.getAvailableTickets())
                .venueId(venue != null ? venue.getId() : null)
                .venueName(venue != null ? venue.getName() : external.getVenueName())
                .externalId(external.getId())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Generate deterministic UUID from external event ID
     * Uses UUID v5 (name-based with SHA-1) for consistent ID generation
     */
    private UUID generateInternalId(String externalId) {
        // Create namespace UUID from constant string
        final UUID namespace = UUID.nameUUIDFromBytes(EXTERNAL_EVENT_NAMESPACE.getBytes());

        // Generate deterministic UUID from external ID within namespace
        return UUID.nameUUIDFromBytes((namespace.toString() + ":" + externalId).getBytes());
    }

    /**
     * Map external event status to internal event status
     * External: AVAILABLE, SOLD_OUT, CANCELLED
     * Internal: PUBLISHED, CANCELLED, etc.
     */
    private EventStatus mapStatus(String externalStatus) {
        if (externalStatus == null) {
            return EventStatus.PUBLISHED;
        }

        return switch (externalStatus) {
            case "AVAILABLE" -> EventStatus.PUBLISHED;
            case "SOLD_OUT" -> EventStatus.PUBLISHED; // Still published, just no tickets available
            case "CANCELLED" -> EventStatus.CANCELLED;
            default -> EventStatus.PUBLISHED;
        };
    }
}
