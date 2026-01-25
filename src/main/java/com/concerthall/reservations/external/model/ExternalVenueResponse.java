package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenueResponse {
    private String id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String description;
    private Integer capacity;
    private List<String> amenities;
    private String imageUrl;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private Coordinates coordinates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinates {
        private Double latitude;
        private Double longitude;
    }
}
