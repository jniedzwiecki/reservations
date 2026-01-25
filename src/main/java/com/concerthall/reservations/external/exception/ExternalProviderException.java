package com.concerthall.reservations.external.exception;

public class ExternalProviderException extends RuntimeException {

    private final String errorCode;

    public ExternalProviderException(String message) {
        super(message);
        this.errorCode = null;
    }

    public ExternalProviderException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public ExternalProviderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ExternalProviderException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
