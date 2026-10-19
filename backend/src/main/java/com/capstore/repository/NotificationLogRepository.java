package com.capstore.repository;

import com.capstore.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO notification_logs (order_id, notification_type, awb, created_at)
            VALUES (:orderId, :type, :awb, CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    int claim(@Param("orderId") Long orderId, @Param("type") String type, @Param("awb") String awb);

    @Modifying
    @Query("delete from NotificationLog n where n.orderId = :orderId and n.type = :type and n.awb = :awb")
    int release(@Param("orderId") Long orderId, @Param("type") String type, @Param("awb") String awb);
}
