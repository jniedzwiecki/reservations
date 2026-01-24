package com.concerthall.reservations.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignVenueRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Venue ID is required")
    private UUID venueId;
}
