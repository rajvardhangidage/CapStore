package com.capstore.repository;

import com.capstore.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO tracking_events
                (shipment_id, courier_name, event_id, status, description, occurred_at)
            VALUES (:shipmentId, :courierName, :eventId, :status, :description, :occurredAt)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("shipmentId") Long shipmentId,
                       @Param("courierName") String courierName,
                       @Param("eventId") String eventId,
                       @Param("status") String status,
                       @Param("description") String description,
                       @Param("occurredAt") LocalDateTime occurredAt);

    List<TrackingEvent> findByShipmentIdOrderByOccurredAtDescIdDesc(Long shipmentId);
}
