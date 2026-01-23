package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.Ticket;
import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.request.ReserveTicketRequest;
import com.concerthall.reservations.dto.response.TicketResponse;
import com.concerthall.reservations.exception.DuplicateTicketException;
import com.concerthall.reservations.exception.InsufficientCapacityException;
import com.concerthall.reservations.exception.InvalidEventStateException;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.TicketRepository;
import com.concerthall.reservations.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketResponse reserveTicket(final ReserveTicketRequest request, final String userEmail) {
        final User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Lock event row to prevent race conditions
        final Event event = eventRepository.findByIdWithPessimisticLock(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // 2. Validate event is published and in future
        validateEventBookable(event);

        // 3. Check if user already has a ticket for this event
        if (ticketRepository.existsByUserIdAndEventIdAndStatusIn(
                user.getId(), event.getId(),
                Arrays.asList(TicketStatus.PENDING_PAYMENT, TicketStatus.PAID))) {
            throw new DuplicateTicketException("You already have a ticket for this event");
        }

        // 4. Check capacity
        final long reservedCount = ticketRepository.countByEventIdAndStatusIn(
                event.getId(),
                Arrays.asList(TicketStatus.PENDING_PAYMENT, TicketStatus.PAID));
        if (reservedCount >= event.getCapacity()) {
            throw new InsufficientCapacityException("Event is sold out");
        }

        // 5. Create and save ticket
        final Ticket ticket = ticketRepository.save(createTicket(event, user));

        log.info("Ticket {} reserved for event {} by user {}",
                ticket.getTicketNumber(), event.getId(), user.getEmail());

        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(final String userEmail) {
        final User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        final List<Ticket> tickets = ticketRepository.findByUserIdWithEvent(user.getId());
        return tickets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(final UUID id, final String userEmail) {
        final Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        final User user = userRepository.findByEmailWithVenues(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate ticket access
        validateTicketAccess(ticket, user);

        return toResponse(ticket);
    }

    @Transactional
    public void cancelTicket(final UUID id, final String userEmail) {
        final Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        final User user = userRepository.findByEmailWithVenues(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate ticket access
        validateTicketAccess(ticket, user);

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        log.info("Ticket {} cancelled by user {}", id, userEmail);
    }

    private void validateEventBookable(final Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidEventStateException("Event is not available for booking");
        }

        if (event.getEventDateTime().isBefore(LocalDateTime.now())) {
            throw new InvalidEventStateException("Event has already occurred");
        }
    }

    private Ticket createTicket(final Event event, final User user) {
        final String ticketNumber = generateTicketNumber(event);

        return Ticket.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .event(event)
                .price(event.getPrice())
                .status(TicketStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    private String generateTicketNumber(final Event event) {
        final String eventDate = event.getEventDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        final String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TKT-%s-%s", eventDate, uniqueId);
    }

    private void validateTicketAccess(final Ticket ticket, final User user) {
        // User owns the ticket
        if (ticket.getUser().getId().equals(user.getId())) {
            return;
        }

        // Admin has access to all tickets
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        // Power user must have access to the event's venue
        if (user.getRole() == UserRole.POWER_USER) {
            final boolean hasVenueAccess = user.getAssignedVenues().stream()
                    .anyMatch(venue -> venue.getId().equals(ticket.getEvent().getVenue().getId()));
            if (hasVenueAccess) {
                return;
            }
        }

        // If none of the conditions above are met, deny access
        throw new ResourceNotFoundException("Ticket not found");
    }

    private TicketResponse toResponse(final Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .userId(ticket.getUser().getId())
                .userEmail(ticket.getUser().getEmail())
                .eventId(ticket.getEvent().getId())
                .eventName(ticket.getEvent().getName())
                .eventDateTime(ticket.getEvent().getEventDateTime())
                .price(ticket.getPrice())
                .status(ticket.getStatus().name())
                .reservedAt(ticket.getReservedAt())
                .paymentExpiresAt(ticket.getPaymentExpiresAt())
                .build();
    }
}
