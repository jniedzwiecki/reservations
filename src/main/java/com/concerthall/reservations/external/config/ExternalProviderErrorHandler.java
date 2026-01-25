package com.concerthall.reservations.external.config;

import com.concerthall.reservations.external.exception.ExternalProviderConnectionException;
import com.concerthall.reservations.external.exception.ExternalProviderException;
import com.concerthall.reservations.external.exception.ExternalProviderRateLimitException;
import com.concerthall.reservations.external.model.ExternalErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class ExternalProviderErrorHandler implements ResponseErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        final HttpStatus statusCode = (HttpStatus) response.getStatusCode();

        try {
            ExternalErrorResponse errorResponse = objectMapper.readValue(
                    response.getBody(),
                    ExternalErrorResponse.class
            );

            if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                Long retryAfter = getRetryAfter(response);
                throw new ExternalProviderRateLimitException(
                        errorResponse.getMessage(),
                        retryAfter
                );
            }

            throw new ExternalProviderException(
                    errorResponse.getMessage(),
                    errorResponse.getError()
            );

        } catch (IOException e) {
            log.error("Failed to parse error response from external provider", e);
            throw new ExternalProviderConnectionException(
                    "External provider returned error: " + statusCode,
                    e
            );
        }
    }

    private Long getRetryAfter(ClientHttpResponse response) {
        try {
            String retryAfter = response.getHeaders().getFirst("Retry-After");
            return retryAfter != null ? Long.parseLong(retryAfter) : 3600L;
        } catch (NumberFormatException e) {
            return 3600L; // default 1 hour
        }
    }
}
