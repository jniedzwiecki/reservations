package com.concerthall.reservations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime eventDateTime;
    private Integer capacity;
    private BigDecimal price;
    private String status;
    private Long availableTickets;
    private LocalDateTime createdAt;
}
