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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Get all events (customers see PUBLISHED only)")
    public ResponseEntity<List<EventResponse>> getAllEvents(Authentication authentication) {
        boolean customerView = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
        List<EventResponse> events = eventService.getAllEvents(customerView);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID with availability")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Create new event (Admin/Power User only)")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = eventService.createEvent(request);
        return new ResponseEntity<>(event, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Update event (Admin/Power User only)")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        EventResponse event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Delete event (Admin/Power User only)")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Update event status (Admin/Power User only)")
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventStatusRequest request) {
        EventResponse event = eventService.updateEventStatus(id, request);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{id}/sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'POWER_USER')")
    @Operation(summary = "Get event sales statistics (Admin/Power User only)")
    public ResponseEntity<EventSalesResponse> getEventSales(@PathVariable Long id) {
        EventSalesResponse sales = eventService.getEventSales(id);
        return ResponseEntity.ok(sales);
    }
}
