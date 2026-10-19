package com.capstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Getter @Setter @NoArgsConstructor
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentDirection direction = ShipmentDirection.FORWARD;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id")
    private ReturnRequest returnRequest;
    @Column(name = "courier_name", nullable = false, length = 100)
    private String courierName;
    @Column(nullable = false, length = 100)
    private String awb;
    @Column(length = 500)
    private String trackingUrl;
    private LocalDate expectedDelivery;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.CREATED;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
