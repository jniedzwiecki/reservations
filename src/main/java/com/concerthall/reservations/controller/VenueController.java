package com.concerthall.reservations.controller;

import com.concerthall.reservations.dto.request.AssignVenueRequest;
import com.concerthall.reservations.dto.request.CreateVenueRequest;
import com.concerthall.reservations.dto.request.UpdateVenueRequest;
import com.concerthall.reservations.dto.response.VenueResponse;
import com.concerthall.reservations.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Venues", description = "Venue management endpoints")
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Get all venues (filtered by user role)")
    public ResponseEntity<List<VenueResponse>> getAllVenues(@AuthenticationPrincipal final UserDetails userDetails) {
        final List<VenueResponse> venues = venueService.getVenuesForUser(userDetails.getUsername());
        return ResponseEntity.ok(venues);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Get venue by ID")
    public ResponseEntity<VenueResponse> getVenueById(@PathVariable final UUID id) {
        final VenueResponse venue = venueService.getVenueById(id);
        return ResponseEntity.ok(venue);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create venue (Admin only)")
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody final CreateVenueRequest request) {
        final VenueResponse venue = venueService.createVenue(request);
        return new ResponseEntity<>(venue, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update venue (Admin only)")
    public ResponseEntity<VenueResponse> updateVenue(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateVenueRequest request) {
        final VenueResponse venue = venueService.updateVenue(id, request);
        return ResponseEntity.ok(venue);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete venue (Admin only)")
    public ResponseEntity<Void> deleteVenue(@PathVariable final UUID id) {
        venueService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign power user to venue (Admin only)")
    public ResponseEntity<Void> assignUserToVenue(@Valid @RequestBody final AssignVenueRequest request) {
        venueService.assignUserToVenue(request.getUserId(), request.getVenueId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unassign power user from venue (Admin only)")
    public ResponseEntity<Void> unassignUserFromVenue(@Valid @RequestBody final AssignVenueRequest request) {
        venueService.unassignUserFromVenue(request.getUserId(), request.getVenueId());
        return ResponseEntity.noContent().build();
    }
}
