package com.concerthall.reservations.dto.request;

import com.concerthall.reservations.domain.enums.EventStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class UpdateEventRequest {

    private String name;
    private String description;
    private LocalDateTime eventDateTime;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    private EventStatus status;

    private UUID venueId;
}
