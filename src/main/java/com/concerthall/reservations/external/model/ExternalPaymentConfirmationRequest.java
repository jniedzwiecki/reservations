package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPaymentConfirmationRequest {
    private String paymentId;
    private String paymentMethod;
    private Money paidAmount;
    private String transactionId;
}
