package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.Ticket;
import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.domain.enums.TicketStatus;
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
    public TicketResponse reserveTicket(ReserveTicketRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Lock event row to prevent race conditions
        Event event = eventRepository.findByIdWithPessimisticLock(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // 2. Validate event is published and in future
        validateEventBookable(event);

        // 3. Check if user already has a ticket for this event
        if (ticketRepository.existsByUserIdAndEventIdAndStatus(
                user.getId(), event.getId(), TicketStatus.RESERVED)) {
            throw new DuplicateTicketException("You already have a ticket for this event");
        }

        // 4. Check capacity
        long reservedCount = ticketRepository.countByEventIdAndStatus(
                event.getId(), TicketStatus.RESERVED);
        if (reservedCount >= event.getCapacity()) {
            throw new InsufficientCapacityException("Event is sold out");
        }

        // 5. Create and save ticket
        Ticket ticket = createTicket(event, user);
        ticket = ticketRepository.save(ticket);

        log.info("Ticket {} reserved for event {} by user {}",
                ticket.getTicketNumber(), event.getId(), user.getEmail());

        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Ticket> tickets = ticketRepository.findByUserIdWithEvent(user.getId());
        return tickets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, String userEmail) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user owns this ticket or is admin/power_user
        if (!ticket.getUser().getId().equals(user.getId()) &&
                !user.getRole().name().equals("ADMIN") &&
                !user.getRole().name().equals("POWER_USER")) {
            throw new ResourceNotFoundException("Ticket not found");
        }

        return toResponse(ticket);
    }

    @Transactional
    public void cancelTicket(Long id, String userEmail) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user owns this ticket or is admin/power_user
        if (!ticket.getUser().getId().equals(user.getId()) &&
                !user.getRole().name().equals("ADMIN") &&
                !user.getRole().name().equals("POWER_USER")) {
            throw new ResourceNotFoundException("Ticket not found");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        log.info("Ticket {} cancelled by user {}", id, userEmail);
    }

    private void validateEventBookable(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidEventStateException("Event is not available for booking");
        }

        if (event.getEventDateTime().isBefore(LocalDateTime.now())) {
            throw new InvalidEventStateException("Event has already occurred");
        }
    }

    private Ticket createTicket(Event event, User user) {
        String ticketNumber = generateTicketNumber(event);

        return Ticket.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .event(event)
                .price(event.getPrice())
                .status(TicketStatus.RESERVED)
                .build();
    }

    private String generateTicketNumber(Event event) {
        String eventDate = event.getEventDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TKT-%s-%s", eventDate, uniqueId);
    }

    private TicketResponse toResponse(Ticket ticket) {
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
                .build();
    }
}
