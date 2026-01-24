package com.concerthall.reservations.dto.response;

import com.concerthall.reservations.domain.enums.VenueSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {
    private UUID id;
    private String name;
    private String address;
    private String description;
    private Integer capacity;
    private VenueSource source;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
