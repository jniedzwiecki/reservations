package com.concerthall.reservations.integration;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.enums.EventStatus;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.request.ReserveTicketRequest;
import com.concerthall.reservations.dto.response.TicketResponse;
import com.concerthall.reservations.exception.DuplicateTicketException;
import com.concerthall.reservations.exception.InsufficientCapacityException;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.TicketRepository;
import com.concerthall.reservations.repository.UserRepository;
import com.concerthall.reservations.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TicketReservationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Event testEvent;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Create test event with limited capacity
        testEvent = Event.builder()
                .name("Test Concert")
                .description("Integration test concert")
                .eventDateTime(LocalDateTime.now().plusDays(7))
                .capacity(5)
                .price(BigDecimal.valueOf(50.00))
                .status(EventStatus.PUBLISHED)
                .build();
        testEvent = eventRepository.save(testEvent);

        // Create test users
        testUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final User user = User.builder()
                    .email("user" + i + "@test.com")
                    .password(passwordEncoder.encode("password"))
                    .role(UserRole.CUSTOMER)
                    .isRemovable(true)
                    .build();
            testUsers.add(userRepository.save(user));
        }
    }

    @Test
    void reserveTicket_Success() {
        final ReserveTicketRequest request = ReserveTicketRequest.builder()
                .eventId(testEvent.getId())
                .build();

        final TicketResponse response = ticketService.reserveTicket(request, testUsers.get(0).getEmail());

        assertNotNull(response);
        assertEquals(testEvent.getName(), response.getEventName());
        assertEquals(testUsers.get(0).getEmail(), response.getUserEmail());
        assertNotNull(response.getTicketNumber());
    }

    @Test
    void reserveTicket_DuplicateThrowsException() {
        final ReserveTicketRequest request = ReserveTicketRequest.builder()
                .eventId(testEvent.getId())
                .build();

        // First reservation succeeds
        ticketService.reserveTicket(request, testUsers.get(0).getEmail());

        // Second reservation for same user and event should fail
        assertThrows(DuplicateTicketException.class, () ->
                ticketService.reserveTicket(request, testUsers.get(0).getEmail()));
    }

    @Test
    void reserveTicket_ConcurrentBookingsRespectCapacity() throws InterruptedException, ExecutionException {
        // Try to book 10 tickets concurrently for an event with capacity 5
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final List<Future<TicketResponse>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int userIndex = i;
            final Future<TicketResponse> future = executor.submit(() -> {
                try {
                    final ReserveTicketRequest request = ReserveTicketRequest.builder()
                            .eventId(testEvent.getId())
                            .build();
                    return ticketService.reserveTicket(request, testUsers.get(userIndex).getEmail());
                } catch (final InsufficientCapacityException e) {
                    return null; // Expected for some threads
                }
            });
            futures.add(future);
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Count successful reservations
        int successCount = 0;
        int failureCount = 0;

        for (final Future<TicketResponse> future : futures) {
            final TicketResponse response = future.get();
            if (response != null) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        // Verify that exactly 5 tickets were sold (matching capacity)
        assertEquals(5, successCount, "Should have exactly 5 successful reservations");
        assertEquals(5, failureCount, "Should have exactly 5 failed reservations");

        // Verify database state
        final long reservedCount = ticketRepository.countByEventIdAndStatus(testEvent.getId(), TicketStatus.RESERVED);
        assertEquals(5, reservedCount, "Database should have exactly 5 reserved tickets");
    }

    @Test
    void reserveTicket_CapacityExceeded() {
        final ReserveTicketRequest request = ReserveTicketRequest.builder()
                .eventId(testEvent.getId())
                .build();

        // Book all 5 available tickets
        for (int i = 0; i < 5; i++) {
            ticketService.reserveTicket(request, testUsers.get(i).getEmail());
        }

        // Attempt to book 6th ticket should fail
        assertThrows(InsufficientCapacityException.class, () ->
                ticketService.reserveTicket(request, testUsers.get(5).getEmail()));
    }

    @Test
    void cancelTicket_FreesCapacity() {
        final ReserveTicketRequest request = ReserveTicketRequest.builder()
                .eventId(testEvent.getId())
                .build();

        // Book 5 tickets (full capacity)
        final List<TicketResponse> tickets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tickets.add(ticketService.reserveTicket(request, testUsers.get(i).getEmail()));
        }

        // Verify capacity is full
        assertThrows(InsufficientCapacityException.class, () ->
                ticketService.reserveTicket(request, testUsers.get(5).getEmail()));

        // Cancel one ticket
        ticketService.cancelTicket(tickets.get(0).getId(), testUsers.get(0).getEmail());

        // Now the same user can book again (ticket was cancelled)
        final TicketResponse newTicket = ticketService.reserveTicket(request, testUsers.get(0).getEmail());
        assertNotNull(newTicket);
    }
}
