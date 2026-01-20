package com.concerthall.reservations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSalesResponse {
    private Long eventId;
    private String eventName;
    private Integer capacity;
    private Long ticketsSold;
    private Long availableTickets;
    private BigDecimal revenue;
    private Double occupancyRate;
}
