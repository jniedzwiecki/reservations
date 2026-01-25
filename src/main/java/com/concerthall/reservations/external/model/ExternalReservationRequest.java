package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalReservationRequest {
    private String eventId;
    private String customerEmail;
    private String customerName;
    private Integer quantity;
    private String idempotencyKey;
}
