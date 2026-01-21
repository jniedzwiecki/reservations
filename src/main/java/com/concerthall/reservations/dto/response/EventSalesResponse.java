package com.concerthall.reservations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSalesResponse {
    private UUID eventId;
    private String eventName;
    private Integer capacity;
    private Long ticketsSold;
    private Long availableTickets;
    private BigDecimal revenue;
    private Double occupancyRate;
}
