package com.concerthall.reservations.service.aggregator;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.Venue;
import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.VenueResponse;
import com.concerthall.reservations.external.adapter.ExternalEventAdapter;
import com.concerthall.reservations.external.adapter.ExternalVenueAdapter;
import com.concerthall.reservations.external.client.ExternalVenueProviderClient;
import com.concerthall.reservations.external.exception.ExternalProviderException;
import com.concerthall.reservations.external.model.ExternalEventResponse;
import com.concerthall.reservations.external.model.ExternalVenueResponse;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.TicketRepository;
import com.concerthall.reservations.repository.UserRepository;
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
public class EventAggregatorService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final ExternalVenueProviderClient externalClient;
    private final ExternalEventAdapter eventAdapter;
    private final ExternalVenueAdapter venueAdapter;
    private final VenueAggregatorService venueAggregator;

    /**
     * Get all events from both internal and external sources
     * Filters by user role and customer view
     * Note: Not readOnly because we create placeholders for external events
     */
    @Transactional
    public List<EventResponse> getAllEvents(String userEmail, boolean customerView) {
        final List<EventResponse> internalEvents = getInternalEvents(userEmail, customerView);
        final List<EventResponse> externalEvents = getExternalEvents(customerView);

        return mergeAndSortEvents(internalEvents, externalEvents);
    }

    /**
     * Get event by ID - check both internal and external sources
     * For external events with externalId, fetch fresh data from external API
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        // Look up event in database (includes both internal and external placeholders)
        final Optional<Event> event = eventRepository.findById(id);
        if (event.isEmpty()) {
            log.debug("Event {} not found in database", id);
            return null;
        }

        // If it's an external event (has externalId), fetch fresh data from external API
        if (event.get().getExternalId() != null) {
            log.debug("Fetching fresh data for external event {} (externalId: {})",
                     id, event.get().getExternalId());
            return getEventByExternalId(event.get().getExternalId());
        }

        // Internal event - return from database
        return toResponse(event.get());
    }

    /**
     * Get event by external ID
     */
    public EventResponse getEventByExternalId(String externalId) {
        try {
            final ExternalEventResponse externalEvent = externalClient.getEventById(externalId);

            // Fetch venue information
            VenueResponse venue = null;
            if (externalEvent.getVenueId() != null) {
                try {
                    final ExternalVenueResponse externalVenue =
                            externalClient.getVenueById(externalEvent.getVenueId());
                    venue = venueAdapter.toVenueResponse(externalVenue);
                } catch (ExternalProviderException e) {
                    log.warn("Failed to fetch venue {} for event {}",
                            externalEvent.getVenueId(), externalId, e);
                }
            }

            return eventAdapter.toEventResponse(externalEvent, venue);
        } catch (ExternalProviderException e) {
            log.error("Failed to fetch external event {}", externalId, e);
            return null;
        }
    }

    /**
     * Fetch internal events from database with role-based filtering
     */
    private List<EventResponse> getInternalEvents(String userEmail, boolean customerView) {
        // Customer view logic - same as original EventService
        if (customerView) {
            return eventRepository.findByStatus(EventStatus.PUBLISHED).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // Get user and apply role-based filtering
        return userRepository.findByEmailWithVenues(userEmail)
                .map(user -> {
                    if (user.getRole() == UserRole.ADMIN) {
                        // Admins see all internal events
                        return eventRepository.findAll().stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
                    } else if (user.getRole() == UserRole.POWER_USER) {
                        // Power users see only events from their assigned venues
                        final List<UUID> venueIds = user.getAssignedVenues().stream()
                                .map(Venue::getId)
                                .collect(Collectors.toList());

                        if (venueIds.isEmpty()) {
                            return Collections.<EventResponse>emptyList();
                        }

                        return eventRepository.findByVenueIdIn(venueIds).stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
                    } else {
                        // Customers see only published events
                        return eventRepository.findByStatus(EventStatus.PUBLISHED).stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
                    }
                })
                .orElse(Collections.emptyList());
    }

    /**
     * Fetch external events from external API
     * Creates placeholder records for UUID mapping
     * Returns empty list if external API fails (graceful degradation)
     * Only returns "published" (available) external events
     */
    private List<EventResponse> getExternalEvents(boolean customerView) {
        try {
            final List<ExternalEventResponse> externalEvents = externalClient.getEvents(null);

            return externalEvents.stream()
                    .filter(e -> customerView ? "AVAILABLE".equals(e.getStatus()) : true)
                    .map(this::findOrCreateEventPlaceholder)
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (ExternalProviderException e) {
            log.error("Failed to fetch external events, returning only internal events", e);
            return Collections.emptyList(); // Graceful degradation
        }
    }

    /**
     * Find existing event placeholder or create new one for external event
     * This creates a UUID -> externalId mapping in the database
     */
    private Event findOrCreateEventPlaceholder(ExternalEventResponse externalEvent) {
        // Check if placeholder already exists
        final Optional<Event> existing = eventRepository.findByExternalId(externalEvent.getId());
        if (existing.isPresent()) {
            // Update placeholder with latest data
            final Event event = existing.get();
            event.setName(externalEvent.getName());
            event.setDescription(externalEvent.getDescription());
            event.setEventDateTime(externalEvent.getEventDateTime());
            event.setCapacity(externalEvent.getCapacity());
            event.setPrice(externalEvent.getPrice().getAmount());
            event.setStatus(eventAdapter.mapStatus(externalEvent.getStatus()));
            return eventRepository.save(event);
        }

        // Ensure venue placeholder exists - fetch by externalId triggers placeholder creation
        final VenueResponse venueResponse = venueAggregator.getVenueByExternalId(externalEvent.getVenueId());
        if (venueResponse == null) {
            log.error("Cannot create event placeholder without venue for event: {}", externalEvent.getId());
            throw new IllegalStateException("Venue placeholder required for event placeholder");
        }

        // Get the venue entity from database
        final Venue venuePlaceholder = venueRepository.findById(venueResponse.getId())
                .orElseThrow(() -> new IllegalStateException("Venue placeholder not found"));

        // Create new placeholder
        final Event newEvent = Event.builder()
                .name(externalEvent.getName())
                .description(externalEvent.getDescription())
                .eventDateTime(externalEvent.getEventDateTime())
                .capacity(externalEvent.getCapacity())
                .price(externalEvent.getPrice().getAmount())
                .status(eventAdapter.mapStatus(externalEvent.getStatus()))
                .venue(venuePlaceholder)
                .externalId(externalEvent.getId())
                .build();

        return eventRepository.save(newEvent);
    }

    /**
     * Merge internal and external events
     * Sort by event date for consistent ordering
     */
    private List<EventResponse> mergeAndSortEvents(
            List<EventResponse> internal,
            List<EventResponse> external
    ) {
        return Stream.concat(internal.stream(), external.stream())
                .sorted(Comparator.comparing(EventResponse::getEventDateTime))
                .collect(Collectors.toList());
    }

    /**
     * Convert internal Event entity to EventResponse
     */
    private EventResponse toResponse(Event event) {
        // Count tickets that are sold (pending payment or paid)
        final long soldCount = ticketRepository.countByEventIdAndStatusIn(
                event.getId(),
                List.of(TicketStatus.PENDING_PAYMENT, TicketStatus.PAID)
        );
        final long availableTickets = event.getCapacity() - soldCount;

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .eventDateTime(event.getEventDateTime())
                .capacity(event.getCapacity())
                .price(event.getPrice())
                .status(event.getStatus().name())
                .availableTickets(availableTickets)
                .venueId(event.getVenue().getId())
                .venueName(event.getVenue().getName())
                .externalId(event.getExternalId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
