package com.concerthall.reservations.dto.request;

import com.concerthall.reservations.domain.enums.EventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventStatusRequest {

    @NotNull(message = "Status is required")
    private EventStatus status;
}
