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
public class UpdatePaymentStatusRequest {

    @NotNull(message = "Status is required")
    private String status;

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    private String transactionId;
}
