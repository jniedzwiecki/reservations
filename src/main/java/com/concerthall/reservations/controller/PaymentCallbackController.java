package com.concerthall.reservations.controller;

import com.concerthall.reservations.domain.Ticket;
import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.dto.request.UpdatePaymentStatusRequest;
import com.concerthall.reservations.dto.response.TicketValidationResponse;
import com.concerthall.reservations.exception.ResourceNotFoundException;
import com.concerthall.reservations.repository.TicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal", description = "Internal service-to-service endpoints")
public class PaymentCallbackController {

    private final TicketRepository ticketRepository;

    @Value("${service.api.key}")
    private String serviceApiKey;

    @GetMapping("/{ticketId}/validate")
    @Operation(summary = "Validate ticket for payment", description = "Internal endpoint for payment service to validate ticket")
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @PathVariable UUID ticketId,
            @RequestHeader("X-API-Key") String apiKey) {

        validateApiKey(apiKey);

        log.info("Validating ticket {} for payment", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        TicketValidationResponse response = TicketValidationResponse.builder()
                .id(ticket.getId())
                .status(ticket.getStatus().name())
                .price(ticket.getPrice())
                .userId(ticket.getUser().getId())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{ticketId}/payment-status")
    @Operation(summary = "Update ticket payment status", description = "Internal endpoint for payment service to update ticket status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable UUID ticketId,
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {

        validateApiKey(apiKey);

        log.info("Updating ticket {} payment status to {}", ticketId, request.getStatus());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        TicketStatus newStatus = TicketStatus.valueOf(request.getStatus());
        ticket.setStatus(newStatus);

        // Clear payment expiration if payment is completed
        if (newStatus == TicketStatus.PAID) {
            ticket.setPaymentExpiresAt(null);
        }

        ticketRepository.save(ticket);

        log.info("Ticket {} status updated to {}", ticketId, newStatus);

        return ResponseEntity.noContent().build();
    }

    private void validateApiKey(String apiKey) {
        if (!serviceApiKey.equals(apiKey)) {
            log.warn("Invalid API key provided");
            throw new RuntimeException("Unauthorized");
        }
    }
}
