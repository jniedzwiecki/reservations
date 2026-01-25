package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAvailabilityResponse {
    private String eventId;
    private Long availableTickets;
    private Integer capacity;
    private String status; // AVAILABLE, SOLD_OUT, CANCELLED
    private LocalDateTime lastUpdated;
}
