package com.concerthall.reservations.repository;

import com.concerthall.reservations.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    Optional<Venue> findByName(String name);

    boolean existsByName(String name);

    Optional<Venue> findByExternalId(String externalId);
}
