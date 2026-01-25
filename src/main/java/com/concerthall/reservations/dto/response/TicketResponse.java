package com.concerthall.reservations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private UUID id;
    private String ticketNumber;
    private UUID userId;
    private String userEmail;
    private UUID eventId;
    private String eventName;
    private LocalDateTime eventDateTime;
    private UUID venueId;
    private String venueName;
    private BigDecimal price;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime paymentExpiresAt;
    private String externalReservationId;
    private String externalConfirmationCode;
}
