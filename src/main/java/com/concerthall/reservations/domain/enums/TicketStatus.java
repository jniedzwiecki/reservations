package com.concerthall.reservations.domain.enums;

public enum TicketStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    RESERVED,  // Kept for backward compatibility
    CANCELLED
}
