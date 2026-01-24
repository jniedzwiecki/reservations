package com.concerthall.reservations.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVenueRequest {

    private String name;

    private String address;

    private String description;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}
