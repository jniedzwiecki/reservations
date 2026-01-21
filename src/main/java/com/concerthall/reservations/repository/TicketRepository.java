package com.concerthall.reservations.repository;

import com.concerthall.reservations.domain.Ticket;
import com.concerthall.reservations.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByUserIdAndEventIdAndStatus(UUID userId, UUID eventId, TicketStatus status);

    long countByEventIdAndStatus(UUID eventId, TicketStatus status);

    List<Ticket> findByUserId(UUID userId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.event WHERE t.user.id = :userId ORDER BY t.event.eventDateTime DESC")
    List<Ticket> findByUserIdWithEvent(@Param("userId") UUID userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.id = :eventId AND t.status = :status")
    long countTicketsByEventAndStatus(@Param("eventId") UUID eventId, @Param("status") TicketStatus status);
}
