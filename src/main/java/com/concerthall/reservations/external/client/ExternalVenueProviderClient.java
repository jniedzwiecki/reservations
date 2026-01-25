package com.concerthall.reservations.external.client;

import com.concerthall.reservations.external.config.ExternalProviderProperties;
import com.concerthall.reservations.external.exception.ExternalProviderConnectionException;
import com.concerthall.reservations.external.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "external-provider.venue-api", name = "enabled", havingValue = "true")
public class ExternalVenueProviderClient {

    private final RestTemplate externalProviderRestTemplate;
    private final ExternalProviderProperties properties;

    /**
     * List all venues from external provider
     * GET /venues
     */
    public List<ExternalVenueResponse> getVenues(Map<String, String> filters) {
        try {
            final String url = buildUrl("/venues", filters);
            log.debug("Fetching venues from external provider: {}", url);

            ResponseEntity<PaginatedResponse<ExternalVenueResponse>> response =
                    externalProviderRestTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            return response.getBody() != null ? response.getBody().getData() : List.of();

        } catch (RestClientException e) {
            log.error("Failed to fetch venues from external provider", e);
            throw new ExternalProviderConnectionException(
                    "Failed to fetch venues from external provider",
                    e
            );
        }
    }

    /**
     * Get venue details by ID
     * GET /venues/{venueId}
     */
    public ExternalVenueResponse getVenueById(String venueId) {
        try {
            final String url = buildUrl("/venues/" + venueId, null);
            log.debug("Fetching venue {} from external provider", venueId);

            ResponseEntity<ExternalVenueResponse> response =
                    externalProviderRestTemplate.getForEntity(
                            url,
                            ExternalVenueResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to fetch venue {} from external provider", venueId, e);
            throw new ExternalProviderConnectionException(
                    "Failed to fetch venue from external provider",
                    e
            );
        }
    }

    /**
     * List events from external provider
     * GET /events
     */
    public List<ExternalEventResponse> getEvents(Map<String, String> filters) {
        try {
            final String url = buildUrl("/events", filters);
            log.debug("Fetching events from external provider: {}", url);

            ResponseEntity<PaginatedResponse<ExternalEventResponse>> response =
                    externalProviderRestTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            return response.getBody() != null ? response.getBody().getData() : List.of();

        } catch (RestClientException e) {
            log.error("Failed to fetch events from external provider", e);
            throw new ExternalProviderConnectionException(
                    "Failed to fetch events from external provider",
                    e
            );
        }
    }

    /**
     * Get event details by ID
     * GET /events/{eventId}
     */
    public ExternalEventResponse getEventById(String eventId) {
        try {
            final String url = buildUrl("/events/" + eventId, null);
            log.debug("Fetching event {} from external provider", eventId);

            ResponseEntity<ExternalEventResponse> response =
                    externalProviderRestTemplate.getForEntity(
                            url,
                            ExternalEventResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to fetch event {} from external provider", eventId, e);
            throw new ExternalProviderConnectionException(
                    "Failed to fetch event from external provider",
                    e
            );
        }
    }

    /**
     * Check availability for an event
     * GET /events/{eventId}/availability
     */
    public ExternalAvailabilityResponse checkAvailability(String eventId) {
        try {
            final String url = buildUrl("/events/" + eventId + "/availability", null);
            log.debug("Checking availability for event {} from external provider", eventId);

            ResponseEntity<ExternalAvailabilityResponse> response =
                    externalProviderRestTemplate.getForEntity(
                            url,
                            ExternalAvailabilityResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to check availability for event {} from external provider", eventId, e);
            throw new ExternalProviderConnectionException(
                    "Failed to check availability from external provider",
                    e
            );
        }
    }

    /**
     * Create a reservation
     * POST /reservations
     */
    public ExternalReservationResponse createReservation(ExternalReservationRequest request) {
        try {
            final String url = buildUrl("/reservations", null);
            log.debug("Creating reservation for event {} from external provider", request.getEventId());

            ResponseEntity<ExternalReservationResponse> response =
                    externalProviderRestTemplate.postForEntity(
                            url,
                            request,
                            ExternalReservationResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to create reservation from external provider", e);
            throw new ExternalProviderConnectionException(
                    "Failed to create reservation from external provider",
                    e
            );
        }
    }

    /**
     * Get reservation details
     * GET /reservations/{reservationId}
     */
    public ExternalReservationResponse getReservation(String reservationId) {
        try {
            final String url = buildUrl("/reservations/" + reservationId, null);
            log.debug("Fetching reservation {} from external provider", reservationId);

            ResponseEntity<ExternalReservationResponse> response =
                    externalProviderRestTemplate.getForEntity(
                            url,
                            ExternalReservationResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to fetch reservation {} from external provider", reservationId, e);
            throw new ExternalProviderConnectionException(
                    "Failed to fetch reservation from external provider",
                    e
            );
        }
    }

    /**
     * Confirm payment for a reservation
     * POST /reservations/{reservationId}/confirm-payment
     */
    public ExternalReservationResponse confirmPayment(
            String reservationId,
            ExternalPaymentConfirmationRequest request
    ) {
        try {
            final String url = buildUrl("/reservations/" + reservationId + "/confirm-payment", null);
            log.debug("Confirming payment for reservation {} with external provider", reservationId);

            ResponseEntity<ExternalReservationResponse> response =
                    externalProviderRestTemplate.postForEntity(
                            url,
                            request,
                            ExternalReservationResponse.class
                    );

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Failed to confirm payment for reservation {} with external provider", reservationId, e);
            throw new ExternalProviderConnectionException(
                    "Failed to confirm payment with external provider",
                    e
            );
        }
    }

    /**
     * Cancel a reservation
     * DELETE /reservations/{reservationId}
     */
    public void cancelReservation(String reservationId) {
        try {
            final String url = buildUrl("/reservations/" + reservationId, null);
            log.debug("Cancelling reservation {} with external provider", reservationId);

            externalProviderRestTemplate.delete(url);

        } catch (RestClientException e) {
            log.error("Failed to cancel reservation {} with external provider", reservationId, e);
            throw new ExternalProviderConnectionException(
                    "Failed to cancel reservation with external provider",
                    e
            );
        }
    }

    /**
     * Get customer reservations
     * GET /reservations?customerEmail={email}
     */
    public List<ExternalReservationResponse> getCustomerReservations(String customerEmail) {
        try {
            final Map<String, String> filters = new HashMap<>();
            filters.put("customerEmail", customerEmail);
            final String url = buildUrl("/reservations", filters);
            log.debug("Fetching reservations for customer {} from external provider", customerEmail);

            ResponseEntity<PaginatedResponse<ExternalReservationResponse>> response =
                    externalProviderRestTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            return response.getBody() != null ? response.getBody().getData() : List.of();

        } catch (RestClientException e) {
            log.error("Failed to fetch customer reservations from external provider", e);
            throw new ExternalProviderConnectionException(
                    "Failed to fetch customer reservations from external provider",
                    e
            );
        }
    }

    /**
     * Build URL with query parameters
     */
    private String buildUrl(String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUrl())
                .path(path);

        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        return builder.toUriString();
    }
}
