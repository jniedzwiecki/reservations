package com.concerthall.reservations.external.exception;

public class ExternalProviderConnectionException extends ExternalProviderException {

    public ExternalProviderConnectionException(String message) {
        super(message);
    }

    public ExternalProviderConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
