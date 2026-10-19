package com.capstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_order_type_awb", columnNames = {"order_id", "notification_type", "awb"}))
@Getter @Setter @NoArgsConstructor
public class NotificationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "notification_type", nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 100)
    private String awb = "";
    @CreationTimestamp
    private LocalDateTime createdAt;
}
