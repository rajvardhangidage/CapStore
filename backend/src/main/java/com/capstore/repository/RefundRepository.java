package com.capstore.repository;

import com.capstore.entity.Refund;
import com.capstore.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByReturnRequestId(Long returnRequestId);
    Optional<Refund> findByGatewayRefundId(String gatewayRefundId);
    List<Refund> findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(RefundStatus status, int attempts);

    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(r.amount), 0) from Refund r
            where r.returnRequest.order.id = :orderId and r.status = com.capstore.entity.RefundStatus.REFUNDED
            """)
    java.math.BigDecimal sumRefundedByOrderId(@org.springframework.data.repository.query.Param("orderId") Long orderId);
}
