package com.concerthall.reservations.service;

import com.concerthall.reservations.domain.User;
import com.concerthall.reservations.domain.Venue;
import com.concerthall.reservations.domain.enums.UserRole;
import com.concerthall.reservations.dto.request.CreateVenueRequest;
import com.concerthall.reservations.dto.request.UpdateVenueRequest;
import com.concerthall.reservations.dto.response.VenueResponse;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.repository.EventRepository;
import com.concerthall.reservations.repository.UserRepository;
import com.concerthall.reservations.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getVenuesForUser(final String email) {
        final User user = userRepository.findByEmailWithVenues(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Admin sees all venues
        if (user.getRole() == UserRole.ADMIN) {
            return getAllVenues();
        }

        // Power user sees only assigned venues
        if (user.getRole() == UserRole.POWER_USER) {
            return user.getAssignedVenues().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // Customers don't have venue access
        return List.of();
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(final UUID id) {
        final Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        return toResponse(venue);
    }

    @Transactional
    public VenueResponse createVenue(final CreateVenueRequest request) {
        if (venueRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Venue with this name already exists");
        }

        final Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .build();

        final Venue savedVenue = venueRepository.save(venue);
        log.info("Venue created: {}", savedVenue.getName());

        return toResponse(savedVenue);
    }

    @Transactional
    public VenueResponse updateVenue(final UUID id, final UpdateVenueRequest request) {
        final Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        if (request.getName() != null) {
            // Check if name is being changed and if new name already exists
            if (!venue.getName().equals(request.getName()) &&
                venueRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Venue with this name already exists");
            }
            venue.setName(request.getName());
        }

        if (request.getAddress() != null) {
            venue.setAddress(request.getAddress());
        }

        if (request.getDescription() != null) {
            venue.setDescription(request.getDescription());
        }

        if (request.getCapacity() != null) {
            venue.setCapacity(request.getCapacity());
        }

        final Venue updatedVenue = venueRepository.save(venue);
        log.info("Venue updated: {}", updatedVenue.getName());

        return toResponse(updatedVenue);
    }

    @Transactional
    public void deleteVenue(final UUID id) {
        final Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        // Check if venue has any events
        if (!venue.getEvents().isEmpty()) {
            throw new IllegalStateException("Cannot delete venue with existing events");
        }

        venueRepository.delete(venue);
        log.info("Venue deleted: {}", venue.getName());
    }

    @Transactional
    public void assignUserToVenue(final UUID userId, final UUID venueId) {
        final User user = userRepository.findByIdWithVenues(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != UserRole.POWER_USER) {
            throw new IllegalArgumentException("Only power users can be assigned to venues");
        }

        final Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        user.getAssignedVenues().add(venue);
        userRepository.save(user);

        log.info("User {} assigned to venue {}", user.getEmail(), venue.getName());
    }

    @Transactional
    public void unassignUserFromVenue(final UUID userId, final UUID venueId) {
        final User user = userRepository.findByIdWithVenues(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        final Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        user.getAssignedVenues().remove(venue);
        userRepository.save(user);

        log.info("User {} unassigned from venue {}", user.getEmail(), venue.getName());
    }

    private VenueResponse toResponse(final Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .description(venue.getDescription())
                .capacity(venue.getCapacity())
                .source(venue.getSource())
                .externalId(venue.getExternalId())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}
