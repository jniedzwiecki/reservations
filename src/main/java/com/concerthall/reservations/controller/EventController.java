package com.concerthall.reservations.controller;

import com.concerthall.reservations.dto.request.CreateEventRequest;
import com.concerthall.reservations.dto.request.UpdateEventRequest;
import com.concerthall.reservations.dto.request.UpdateEventStatusRequest;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.EventSalesResponse;
import com.concerthall.reservations.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Get all events (customers see PUBLISHED only)")
    public ResponseEntity<List<EventResponse>> getAllEvents(
            final Authentication authentication,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final boolean customerView = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
        final List<EventResponse> events = eventService.getAllEvents(
                userDetails != null ? userDetails.getUsername() : null,
                customerView);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID with availability")
    public ResponseEntity<EventResponse> getEventById(@PathVariable final UUID id) {
        final EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Create new event (Admin/Power User only)")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody final CreateEventRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final EventResponse event = eventService.createEvent(request, userDetails.getUsername());
        return new ResponseEntity<>(event, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Update event (Admin/Power User only)")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateEventRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final EventResponse event = eventService.updateEvent(id, request, userDetails.getUsername());
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Delete event (Admin/Power User only)")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable final UUID id,
            @AuthenticationPrincipal final UserDetails userDetails) {
        eventService.deleteEvent(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Update event status (Admin/Power User only)")
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateEventStatusRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final EventResponse event = eventService.updateEventStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{id}/sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Get event sales statistics (Admin/Power User only)")
    public ResponseEntity<EventSalesResponse> getEventSales(
            @PathVariable final UUID id,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final EventSalesResponse sales = eventService.getEventSales(id, userDetails.getUsername());
        return ResponseEntity.ok(sales);
    }
}
