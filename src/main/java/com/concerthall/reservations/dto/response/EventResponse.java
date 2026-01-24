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
public class EventResponse {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime eventDateTime;
    private Integer capacity;
    private BigDecimal price;
    private String status;
    private Long availableTickets;
    private UUID venueId;
    private String venueName;
    private String externalId;
    private LocalDateTime createdAt;
}
