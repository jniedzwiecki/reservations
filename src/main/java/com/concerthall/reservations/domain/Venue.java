package com.concerthall.reservations.domain;

import com.concerthall.reservations.domain.enums.VenueSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "venues", indexes = {
    @Index(name = "idx_venues_name", columnList = "name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"assignedUsers", "events"})
@EqualsAndHashCode(exclude = {"assignedUsers", "events"})
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VenueSource source = VenueSource.INTERNAL;

    @Column(name = "external_id")
    private String externalId;

    @JsonIgnore
    @ManyToMany(mappedBy = "assignedVenues", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> assignedUsers = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "venue", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Event> events = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
