package com.concerthall.reservations.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveTicketRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;
}
