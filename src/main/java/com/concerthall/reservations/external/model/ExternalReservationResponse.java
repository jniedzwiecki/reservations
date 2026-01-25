package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalReservationResponse {
    private String id;
    private String eventId;
    private String eventName;
    private LocalDateTime eventDateTime;
    private String venueId;
    private String venueName;
    private String venueAddress;
    private String customerEmail;
    private String customerName;
    private Integer quantity;
    private Money pricePerTicket;
    private Money totalPrice;
    private String status; // PENDING_PAYMENT, CONFIRMED, CANCELLED
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime paymentCompletedAt;
    private String confirmationCode;
    private List<String> ticketNumbers;
}
