package com.capstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_events", uniqueConstraints =
        @UniqueConstraint(name = "uk_tracking_courier_event", columnNames = {"courier_name", "event_id"}))
@Getter @Setter @NoArgsConstructor
public class TrackingEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    @Column(name = "courier_name", nullable = false, length = 100)
    private String courierName;
    @Column(name = "event_id", nullable = false, length = 190)
    private String eventId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status;
    @Column(length = 500)
    private String description;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
