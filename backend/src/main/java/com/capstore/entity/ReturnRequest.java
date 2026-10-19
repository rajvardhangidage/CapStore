package com.capstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Getter @Setter @NoArgsConstructor
public class ReturnRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;
    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, length = 20)
    private ReturnType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.REQUESTED;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(name = "admin_note", length = 500)
    private String adminNote;
    @Column(name = "qc_note", length = 500)
    private String qcNote;
    @Column(name = "requested_quantity", nullable = false)
    private int quantity;
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;
    @Column(name = "upi_id", length = 100)
    private String upiId;
    @Column(name = "pickup_failures", nullable = false)
    private int pickupFailures;
    @Column(name = "completed_return", nullable = false)
    private boolean completedReturn;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_order_id")
    private CustomerOrder replacementOrder;
    @Version
    private long version;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
