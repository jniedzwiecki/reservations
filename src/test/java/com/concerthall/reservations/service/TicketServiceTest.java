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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    private User testUser;
    private Event testEvent;
    private ReserveTicketRequest reserveRequest;

    @BeforeEach
    void setUp() {
        final UUID userId = UUID.randomUUID();
        final UUID eventId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("password")
                .role(UserRole.CUSTOMER)
                .isRemovable(true)
                .build();

        testEvent = Event.builder()
                .id(eventId)
                .name("Test Concert")
                .description("A test concert")
                .eventDateTime(LocalDateTime.now().plusDays(7))
                .capacity(100)
                .price(BigDecimal.valueOf(50.00))
                .status(EventStatus.PUBLISHED)
                .build();

        reserveRequest = ReserveTicketRequest.builder()
                .eventId(eventId)
                .build();
    }

    @Test
    void reserveTicket_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(eventRepository.findByIdWithPessimisticLock(testEvent.getId())).thenReturn(Optional.of(testEvent));
        when(ticketRepository.existsByUserIdAndEventIdAndStatus(any(UUID.class), any(UUID.class), any())).thenReturn(false);
        when(ticketRepository.countByEventIdAndStatus(any(UUID.class), any())).thenReturn(50L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            final Ticket ticket = invocation.getArgument(0);
            ticket.setId(UUID.randomUUID());
            return ticket;
        });

        final TicketResponse response = ticketService.reserveTicket(reserveRequest, testUser.getEmail());

        assertNotNull(response);
        assertEquals(testEvent.getName(), response.getEventName());
        assertEquals(testUser.getEmail(), response.getUserEmail());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void reserveTicket_EventNotFound() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(eventRepository.findByIdWithPessimisticLock(testEvent.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                ticketService.reserveTicket(reserveRequest, testUser.getEmail()));
    }

    @Test
    void reserveTicket_EventNotPublished() {
        testEvent.setStatus(EventStatus.DRAFT);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(eventRepository.findByIdWithPessimisticLock(testEvent.getId())).thenReturn(Optional.of(testEvent));

        assertThrows(InvalidEventStateException.class, () ->
                ticketService.reserveTicket(reserveRequest, testUser.getEmail()));
    }

    @Test
    void reserveTicket_EventInPast() {
        testEvent.setEventDateTime(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(eventRepository.findByIdWithPessimisticLock(testEvent.getId())).thenReturn(Optional.of(testEvent));

        assertThrows(InvalidEventStateException.class, () ->
                ticketService.reserveTicket(reserveRequest, testUser.getEmail()));
    }

    @Test
    void reserveTicket_DuplicateTicket() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(eventRepository.findByIdWithPessimisticLock(testEvent.getId())).thenReturn(Optional.of(testEvent));
        when(ticketRepository.existsByUserIdAndEventIdAndStatus(
                testUser.getId(), testEvent.getId(), TicketStatus.RESERVED)).thenReturn(true);

        assertThrows(DuplicateTicketException.class, () ->
                ticketService.reserveTicket(reserveRequest, testUser.getEmail()));
    }

    @Test
    void reserveTicket_InsufficientCapacity() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(eventRepository.findByIdWithPessimisticLock(testEvent.getId())).thenReturn(Optional.of(testEvent));
        when(ticketRepository.existsByUserIdAndEventIdAndStatus(any(UUID.class), any(UUID.class), any())).thenReturn(false);
        when(ticketRepository.countByEventIdAndStatus(testEvent.getId(), TicketStatus.RESERVED))
                .thenReturn(100L); // Event at capacity

        assertThrows(InsufficientCapacityException.class, () ->
                ticketService.reserveTicket(reserveRequest, testUser.getEmail()));
    }

    @Test
    void cancelTicket_Success() {
        final UUID ticketId = UUID.randomUUID();
        final Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ticketNumber("TKT-20260127-ABC123")
                .user(testUser)
                .event(testEvent)
                .price(testEvent.getPrice())
                .status(TicketStatus.RESERVED)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        assertDoesNotThrow(() -> ticketService.cancelTicket(ticketId, testUser.getEmail()));

        verify(ticketRepository).save(argThat(t -> t.getStatus() == TicketStatus.CANCELLED));
    }

    @Test
    void cancelTicket_NotOwner() {
        final UUID otherUserId = UUID.randomUUID();
        final UUID ticketId = UUID.randomUUID();
        final User otherUser = User.builder()
                .id(otherUserId)
                .email("other@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        final Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(otherUser)
                .event(testEvent)
                .status(TicketStatus.RESERVED)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        assertThrows(ResourceNotFoundException.class, () ->
                ticketService.cancelTicket(ticketId, testUser.getEmail()));
    }
}
