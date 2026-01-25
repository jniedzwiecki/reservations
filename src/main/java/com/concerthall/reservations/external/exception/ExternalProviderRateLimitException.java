package com.concerthall.reservations.external.exception;

public class ExternalProviderRateLimitException extends ExternalProviderException {

    private final Long retryAfter; // seconds until retry

    public ExternalProviderRateLimitException(String message, Long retryAfter) {
        super(message, "RATE_LIMIT_EXCEEDED");
        this.retryAfter = retryAfter;
    }

    public Long getRetryAfter() {
        return retryAfter;
    }
}
