package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.Ticket;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationService {

    private final TicketRepository ticketRepository;

    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void expireUnpaidTickets() {
        final LocalDateTime now = LocalDateTime.now();
        final List<Ticket> expired = ticketRepository
                .findByStatusAndPaymentExpiresAtBefore(
                        TicketStatus.PENDING_PAYMENT, now);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} expired tickets to cancel", expired.size());

        expired.forEach(ticket -> {
            log.info("Expiring ticket {} (payment deadline: {})",
                    ticket.getTicketNumber(), ticket.getPaymentExpiresAt());
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        });

        log.info("Successfully cancelled {} expired tickets", expired.size());
    }
}
