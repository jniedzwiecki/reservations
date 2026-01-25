package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalEventResponse {
    private String id;
    private String venueId;
    private String venueName;
    private String name;
    private String description;
    private LocalDateTime eventDateTime;
    private Integer duration; // in minutes
    private String category;
    private List<String> performers;
    private Money price;
    private Integer capacity;
    private Long availableTickets;
    private String status; // AVAILABLE, SOLD_OUT, CANCELLED
    private String imageUrl;
    private String ageRestriction;
    private List<String> tags;
    private String seatingInfo;
    private String cancellationPolicy;
}
