package com.concerthall.reservations.repository;

import com.concerthall.reservations.domain.Event;
import com.concerthall.reservations.domain.enums.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatus(EventStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithPessimisticLock(@Param("id") UUID id);

    List<Event> findByVenueIdIn(List<UUID> venueIds);

    List<Event> findByVenueIdInAndStatus(List<UUID> venueIds, EventStatus status);
}
