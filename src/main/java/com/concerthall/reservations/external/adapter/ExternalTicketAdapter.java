package com.concerthall.reservations.external.adapter;

import com.concerthall.reservations.domain.enums.TicketStatus;
import com.concerthall.reservations.dto.response.EventResponse;
import com.concerthall.reservations.dto.response.TicketResponse;
import com.concerthall.reservations.external.model.ExternalReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class ExternalTicketAdapter {

    private static final String EXTERNAL_RESERVATION_NAMESPACE = "external-reservation";

    /**
     * Convert external reservation response to internal ticket response
     */
    public TicketResponse toTicketResponse(
            ExternalReservationResponse reservation,
            EventResponse event,
            UUID userId,
            String userEmail
    ) {
        if (reservation == null) {
            return null;
        }

        return TicketResponse.builder()
                .id(generateInternalId(reservation.getId()))
                .ticketNumber(reservation.getConfirmationCode())
                .userId(userId)
                .userEmail(userEmail)
                .eventId(event != null ? event.getId() : null)
                .eventName(event != null ? event.getName() : reservation.getEventName())
                .eventDateTime(event != null ? event.getEventDateTime() : reservation.getEventDateTime())
                .venueId(event != null ? event.getVenueId() : null)
                .venueName(event != null ? event.getVenueName() : reservation.getVenueName())
                .price(reservation.getTotalPrice() != null ? reservation.getTotalPrice().getAmount() : null)
                .status(mapStatus(reservation.getStatus()).name())
                .reservedAt(reservation.getReservedAt())
                .paymentExpiresAt(reservation.getExpiresAt())
                .externalReservationId(reservation.getId())
                .externalConfirmationCode(reservation.getConfirmationCode())
                .build();
    }

    /**
     * Generate deterministic UUID from external reservation ID
     * Uses UUID v5 (name-based with SHA-1) for consistent ID generation
     */
    private UUID generateInternalId(String externalReservationId) {
        // Create namespace UUID from constant string
        final UUID namespace = UUID.nameUUIDFromBytes(EXTERNAL_RESERVATION_NAMESPACE.getBytes());

        // Generate deterministic UUID from external reservation ID within namespace
        return UUID.nameUUIDFromBytes((namespace.toString() + ":" + externalReservationId).getBytes());
    }

    /**
     * Map external reservation status to internal ticket status
     * External: PENDING_PAYMENT, CONFIRMED, CANCELLED
     * Internal: PENDING_PAYMENT, PAID, CANCELLED
     */
    private TicketStatus mapStatus(String externalStatus) {
        if (externalStatus == null) {
            return TicketStatus.PENDING_PAYMENT;
        }

        return switch (externalStatus) {
            case "PENDING_PAYMENT" -> TicketStatus.PENDING_PAYMENT;
            case "CONFIRMED" -> TicketStatus.PAID;
            case "CANCELLED" -> TicketStatus.CANCELLED;
            default -> TicketStatus.PENDING_PAYMENT;
        };
    }
}
