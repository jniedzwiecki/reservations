package com.concerthall.reservations.controller;

import com.concerthall.reservations.dto.request.ReserveTicketRequest;
import com.concerthall.reservations.dto.response.TicketResponse;
import com.concerthall.reservations.service.TicketService;
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

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tickets", description = "Ticket reservation endpoints")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Reserve a ticket for an event (Customer only)")
    public ResponseEntity<TicketResponse> reserveTicket(
            @Valid @RequestBody ReserveTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse ticket = ticketService.reserveTicket(request, userDetails.getUsername());
        return new ResponseEntity<>(ticket, HttpStatus.CREATED);
    }

    @GetMapping("/my-tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current user's tickets (Customer only)")
    public ResponseEntity<List<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TicketResponse> tickets = ticketService.getMyTickets(userDetails.getUsername());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details (Owner/Admin/Power User only)")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse ticket = ticketService.getTicketById(id, userDetails.getUsername());
        return ResponseEntity.ok(ticket);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel ticket (Owner/Admin/Power User only)")
    public ResponseEntity<Void> cancelTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ticketService.cancelTicket(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
