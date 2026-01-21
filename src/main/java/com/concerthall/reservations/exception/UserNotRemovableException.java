package com.concerthall.reservations.exception;

public class UserNotRemovableException extends RuntimeException {
    public UserNotRemovableException(String message) {
        super(message);
    }
}
