package com.concerthall.reservations.service.aggregator;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.Ticket;
import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.TicketResponse;
import com.concerthall.reservations.exception.InsufficientCapacityException;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.external.adapter.ExternalEventAdapter;
import com.concerthall.reservations.external.adapter.ExternalTicketAdapter;
import com.concerthall.reservations.external.client.ExternalVenueProviderClient;
import com.concerthall.reservations.external.exception.ExternalProviderException;
import com.concerthall.reservations.external.model.*;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.TicketRepository;
import com.concerthall.reservations.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class TicketAggregatorService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ExternalVenueProviderClient externalClient;
    private final ExternalTicketAdapter ticketAdapter;
    private final EventAggregatorService eventAggregator;

    /**
     * Reserve ticket for external event
     * Creates external reservation and local tracking ticket
     */
    @Transactional
    public TicketResponse reserveExternalTicket(String eventExternalId, String userEmail) {
        final User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Check availability with external provider
        final ExternalAvailabilityResponse availability;
        try {
            availability = externalClient.checkAvailability(eventExternalId);
        } catch (ExternalProviderException e) {
            log.error("Failed to check availability for external event {}", eventExternalId, e);
            throw new RuntimeException("Failed to check availability with external provider", e);
        }

        if (availability.getAvailableTickets() < 1) {
            throw new InsufficientCapacityException("Event sold out");
        }

        // 2. Create reservation with external provider
        final ExternalReservationRequest reservationRequest = ExternalReservationRequest.builder()
                .eventId(eventExternalId)
                .customerEmail(userEmail)
                .customerName(getUserName(user))
                .quantity(1)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        final ExternalReservationResponse reservation;
        try {
            reservation = externalClient.createReservation(reservationRequest);
        } catch (ExternalProviderException e) {
            log.error("Failed to create reservation for external event {}", eventExternalId, e);
            throw new RuntimeException("Failed to create reservation with external provider", e);
        }

        // 3. Get event information for local tracking
        final EventResponse event = eventAggregator.getEventByExternalId(eventExternalId);
        if (event == null) {
            // Rollback external reservation
            try {
                externalClient.cancelReservation(reservation.getId());
            } catch (ExternalProviderException e) {
                log.error("Failed to cancel external reservation {} after event lookup failed",
                        reservation.getId(), e);
            }
            throw new ResourceNotFoundException("Event not found");
        }

        // 4. Create local tracking ticket
        final Ticket ticket = Ticket.builder()
                .ticketNumber(reservation.getConfirmationCode())
                .user(user)
                .event(getOrCreateEventPlaceholder(event))
                .price(reservation.getTotalPrice() != null ?
                        reservation.getTotalPrice().getAmount() : BigDecimal.ZERO)
                .status(TicketStatus.PENDING_PAYMENT)
                .paymentExpiresAt(reservation.getExpiresAt())
                .externalReservationId(reservation.getId())
                .externalConfirmationCode(reservation.getConfirmationCode())
                .build();

        final Ticket savedTicket = ticketRepository.save(ticket);

        log.info("External ticket reserved: {} for event {} by user {}",
                savedTicket.getTicketNumber(), eventExternalId, userEmail);

        return toResponse(savedTicket);
    }

    /**
     * Confirm payment with external provider
     * Called after successful payment in our system
     */
    @Transactional
    public void confirmExternalPayment(UUID ticketId, String paymentId) {
        final Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // Only process if this is an external ticket
        if (ticket.getExternalReservationId() == null) {
            log.debug("Ticket {} is not external, skipping external payment confirmation", ticketId);
            return;
        }

        // Confirm payment with external provider
        final ExternalPaymentConfirmationRequest confirmRequest =
                ExternalPaymentConfirmationRequest.builder()
                        .paymentId(paymentId)
                        .paymentMethod("credit_card")
                        .paidAmount(Money.builder()
                                .amount(ticket.getPrice())
                                .currency("USD")
                                .build())
                        .transactionId(paymentId)
                        .build();

        try {
            final ExternalReservationResponse confirmedReservation =
                    externalClient.confirmPayment(ticket.getExternalReservationId(), confirmRequest);

            // Update ticket with confirmation details
            ticket.setStatus(TicketStatus.PAID);
            if (confirmedReservation.getTicketNumbers() != null &&
                    !confirmedReservation.getTicketNumbers().isEmpty()) {
                ticket.setTicketNumber(confirmedReservation.getTicketNumbers().get(0));
            }
            ticketRepository.save(ticket);

            log.info("External payment confirmed for ticket {} with reservation {}",
                    ticketId, ticket.getExternalReservationId());

        } catch (ExternalProviderException e) {
            log.error("Failed to confirm payment with external provider for ticket {}",
                    ticketId, e);
            // Critical: External confirmation failed after payment succeeded
            // This requires manual intervention or automatic refund
            throw new RuntimeException(
                    "Failed to confirm payment with external provider - requires refund", e);
        }
    }

    /**
     * Cancel external reservation
     * Called when user cancels ticket or payment expires
     */
    @Transactional
    public void cancelExternalReservation(UUID ticketId, String userEmail) {
        final Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // Validate user access
        if (!ticket.getUser().getEmail().equals(userEmail)) {
            throw new ResourceNotFoundException("Ticket not found");
        }

        // Only process if this is an external ticket
        if (ticket.getExternalReservationId() == null) {
            log.debug("Ticket {} is not external, skipping external cancellation", ticketId);
            return;
        }

        // Cancel with external provider
        try {
            externalClient.cancelReservation(ticket.getExternalReservationId());
            log.info("External reservation {} cancelled for ticket {}",
                    ticket.getExternalReservationId(), ticketId);
        } catch (ExternalProviderException e) {
            log.error("Failed to cancel external reservation {} for ticket {}",
                    ticket.getExternalReservationId(), ticketId, e);
            // Continue with local cancellation even if external fails
        }

        // Update local ticket
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }

    /**
     * Get all tickets for user from both internal and external sources
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(String userEmail) {
        final User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get internal tickets
        final List<TicketResponse> internalTickets = ticketRepository
                .findByUserIdWithEvent(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Get external tickets (if any exist in external system but not tracked locally)
        final List<TicketResponse> externalTickets = getExternalTickets(userEmail, user.getId());

        return mergeAndSortTickets(internalTickets, externalTickets);
    }

    /**
     * Fetch tickets from external provider
     * Returns empty list if external API fails
     */
    private List<TicketResponse> getExternalTickets(String userEmail, UUID userId) {
        try {
            final List<ExternalReservationResponse> reservations =
                    externalClient.getCustomerReservations(userEmail);

            return reservations.stream()
                    .filter(r -> !isTrackedLocally(r.getId())) // Avoid duplicates
                    .map(reservation -> {
                        // Fetch event information
                        EventResponse event = eventAggregator.getEventByExternalId(
                                reservation.getEventId());
                        return ticketAdapter.toTicketResponse(
                                reservation, event, userId, userEmail);
                    })
                    .collect(Collectors.toList());
        } catch (ExternalProviderException e) {
            log.error("Failed to fetch external tickets for user {}", userEmail, e);
            return Collections.emptyList(); // Graceful degradation
        }
    }

    /**
     * Check if external reservation is already tracked locally
     */
    private boolean isTrackedLocally(String externalReservationId) {
        return ticketRepository.findByExternalReservationId(externalReservationId)
                .isPresent();
    }

    /**
     * Merge and sort tickets by event date
     */
    private List<TicketResponse> mergeAndSortTickets(
            List<TicketResponse> internal,
            List<TicketResponse> external
    ) {
        return Stream.concat(internal.stream(), external.stream())
                .sorted((t1, t2) -> {
                    if (t1.getEventDateTime() == null) return 1;
                    if (t2.getEventDateTime() == null) return -1;
                    return t2.getEventDateTime().compareTo(t1.getEventDateTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get or create event placeholder for external events
     * This ensures we have an Event entity to associate with the Ticket
     */
    private Event getOrCreateEventPlaceholder(EventResponse eventResponse) {
        // Check if we already have this external event
        if (eventResponse.getExternalId() != null) {
            return eventRepository.findByExternalId(eventResponse.getExternalId())
                    .orElseGet(() -> {
                        // Create placeholder - this is a simplified version
                        // In production, you might want to create a full Event entity
                        log.warn("Event placeholder creation not fully implemented for external event {}",
                                eventResponse.getExternalId());
                        throw new UnsupportedOperationException(
                                "External event placeholder creation not implemented");
                    });
        }

        // Internal event
        return eventRepository.findById(eventResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    /**
     * Get user's full name
     */
    private String getUserName(User user) {
        // Assuming email as name for now
        // In production, User entity should have firstName/lastName fields
        return user.getEmail().split("@")[0];
    }

    /**
     * Convert Ticket entity to TicketResponse
     */
    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .userId(ticket.getUser().getId())
                .userEmail(ticket.getUser().getEmail())
                .eventId(ticket.getEvent().getId())
                .eventName(ticket.getEvent().getName())
                .eventDateTime(ticket.getEvent().getEventDateTime())
                .venueId(ticket.getEvent().getVenue().getId())
                .venueName(ticket.getEvent().getVenue().getName())
                .price(ticket.getPrice())
                .status(ticket.getStatus().name())
                .reservedAt(ticket.getReservedAt())
                .paymentExpiresAt(ticket.getPaymentExpiresAt())
                .externalReservationId(ticket.getExternalReservationId())
                .externalConfirmationCode(ticket.getExternalConfirmationCode())
                .build();
    }
}
