package com.capstore.repository;

import com.capstore.entity.Payment;
import com.capstore.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.gatewayOrderId = :gatewayOrderId")
    Optional<Payment> findByGatewayOrderIdForUpdate(@Param("gatewayOrderId") String gatewayOrderId);
    Optional<Payment> findTopByOrderIdAndStatusOrderByIdDesc(Long orderId, PaymentStatus status);
    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.order.id = :orderId and p.status = com.capstore.entity.PaymentStatus.PAID")
    java.math.BigDecimal sumPaidAmountByOrderId(@Param("orderId") Long orderId);
}
