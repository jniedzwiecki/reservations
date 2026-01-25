package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.Venue;
import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.request.CreateEventRequest;
import com.concerthall.reservations.dto.request.UpdateEventRequest;
import com.concerthall.reservations.dto.request.UpdateEventStatusRequest;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.EventSalesResponse;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.exception.VenueAccessDeniedException;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.TicketRepository;
import com.concerthall.reservations.repository.UserRepository;
import com.concerthall.reservations.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(final String userEmail, final boolean customerView) {
        final List<Event> events;

        if (customerView) {
            // Customers see only published events
            events = eventRepository.findByStatus(EventStatus.PUBLISHED);
        } else {
            // For authenticated users, filter by role
            final User user = userRepository.findByEmailWithVenues(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user.getRole() == UserRole.ADMIN) {
                // Admins see all events
                events = eventRepository.findAll();
            } else if (user.getRole() == UserRole.POWER_USER) {
                // Power users see only events from their assigned venues
                final List<UUID> venueIds = user.getAssignedVenues().stream()
                        .map(Venue::getId)
                        .collect(Collectors.toList());

                if (venueIds.isEmpty()) {
                    events = List.of();
                } else {
                    events = eventRepository.findByVenueIdIn(venueIds);
                }
            } else {
                // Customers see only published events
                events = eventRepository.findByStatus(EventStatus.PUBLISHED);
            }
        }

        return events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(final UUID id) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toResponse(event);
    }

    @Transactional
    public EventResponse createEvent(final CreateEventRequest request, final String userEmail) {
        // Validate venue exists
        final Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        // Validate venue access
        validateVenueAccess(request.getVenueId(), userEmail);

        final Event event = eventRepository.save(Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .eventDateTime(request.getEventDateTime())
                .capacity(request.getCapacity())
                .price(request.getPrice())
                .status(request.getStatus())
                .venue(venue)
                .build());

        log.info("Event created: {} with ID {} in venue {}", event.getName(), event.getId(), venue.getName());

        return toResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(final UUID id, final UpdateEventRequest request, final String userEmail) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Validate access to current venue
        validateVenueAccess(event.getVenue().getId(), userEmail);

        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDateTime() != null) {
            event.setEventDateTime(request.getEventDateTime());
        }
        if (request.getCapacity() != null) {
            event.setCapacity(request.getCapacity());
        }
        if (request.getPrice() != null) {
            event.setPrice(request.getPrice());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        if (request.getVenueId() != null) {
            // Validate access to new venue
            validateVenueAccess(request.getVenueId(), userEmail);
            final Venue newVenue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
            event.setVenue(newVenue);
        }

        final Event savedEvent = eventRepository.save(event);
        log.info("Event updated: {}", savedEvent.getId());

        return toResponse(savedEvent);
    }

    @Transactional
    public void deleteEvent(final UUID id, final String userEmail) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Validate venue access
        validateVenueAccess(event.getVenue().getId(), userEmail);

        eventRepository.delete(event);
        log.info("Event deleted: {}", id);
    }

    @Transactional
    public EventResponse updateEventStatus(final UUID id, final UpdateEventStatusRequest request, final String userEmail) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Validate venue access
        validateVenueAccess(event.getVenue().getId(), userEmail);

        event.setStatus(request.getStatus());
        final Event savedEvent = eventRepository.save(event);
        log.info("Event {} status updated to {}", id, request.getStatus());

        return toResponse(savedEvent);
    }

    @Transactional(readOnly = true)
    public EventSalesResponse getEventSales(final UUID id, final String userEmail) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Validate venue access
        validateVenueAccess(event.getVenue().getId(), userEmail);

        // Count tickets that are sold (pending payment or paid)
        final long ticketsSold = ticketRepository.countByEventIdAndStatusIn(
                id,
                List.of(TicketStatus.PENDING_PAYMENT, TicketStatus.PAID)
        );

        // Revenue should only include paid tickets
        final long paidTickets = ticketRepository.countByEventIdAndStatus(id, TicketStatus.PAID);

        final long availableTickets = event.getCapacity() - ticketsSold;
        final BigDecimal revenue = event.getPrice().multiply(BigDecimal.valueOf(paidTickets));
        final double occupancyRate = (ticketsSold * 100.0) / event.getCapacity();

        return EventSalesResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .capacity(event.getCapacity())
                .ticketsSold(ticketsSold)
                .availableTickets(availableTickets)
                .revenue(revenue)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .build();
    }

    private void validateVenueAccess(final UUID venueId, final String userEmail) {
        final User user = userRepository.findByEmailWithVenues(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Admin has access to all venues
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        // Power user must have venue assigned
        if (user.getRole() == UserRole.POWER_USER) {
            final boolean hasAccess = user.getAssignedVenues().stream()
                    .anyMatch(venue -> venue.getId().equals(venueId));

            if (!hasAccess) {
                throw new VenueAccessDeniedException("Access denied to this venue");
            }
        } else {
            // Customers cannot manage events
            throw new VenueAccessDeniedException("Insufficient permissions");
        }
    }

    private EventResponse toResponse(final Event event) {
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
