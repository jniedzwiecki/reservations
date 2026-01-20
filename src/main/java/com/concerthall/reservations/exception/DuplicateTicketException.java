package com.concerthall.reservations.exception;

public class DuplicateTicketException extends RuntimeException {
    public DuplicateTicketException(String message) {
        super(message);
    }
}
