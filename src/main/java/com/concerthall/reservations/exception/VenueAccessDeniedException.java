package com.concerthall.reservations.exception;

public class VenueAccessDeniedException extends RuntimeException {
    public VenueAccessDeniedException(String message) {
        super(message);
    }
}
