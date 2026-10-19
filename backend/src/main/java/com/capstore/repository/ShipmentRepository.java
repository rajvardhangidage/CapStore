package com.capstore.repository;

import com.capstore.entity.Shipment;
import com.capstore.entity.ShipmentDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.order.id = :orderId and s.direction = com.capstore.entity.ShipmentDirection.FORWARD")
    Optional<Shipment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.courierName = :courierName and s.awb = :awb")
    Optional<Shipment> findByCourierNameAndAwbForUpdate(@Param("courierName") String courierName,
                                                        @Param("awb") String awb);

    Optional<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findFirstByOrderIdAndDirectionAndReturnRequestIsNull(Long orderId, ShipmentDirection direction);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.order.id = :orderId and s.direction = :direction")
    Optional<Shipment> findByOrderIdAndDirectionForUpdate(@Param("orderId") Long orderId,
                                                          @Param("direction") ShipmentDirection direction);
    Optional<Shipment> findFirstByReturnRequestIdAndDirectionOrderByIdDesc(Long returnRequestId,
                                                                           ShipmentDirection direction);
    Optional<Shipment> findByCourierNameAndAwb(String courierName, String awb);
}
