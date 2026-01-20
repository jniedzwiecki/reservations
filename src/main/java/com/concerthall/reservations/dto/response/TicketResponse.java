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
public class TicketResponse {
    private Long id;
    private String ticketNumber;
    private Long userId;
    private String userEmail;
    private Long eventId;
    private String eventName;
    private LocalDateTime eventDateTime;
    private BigDecimal price;
    private String status;
    private LocalDateTime reservedAt;
}
