package com.capstore.repository;

import com.capstore.entity.OrderStatusHistory;
import com.capstore.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByIdAsc(Long orderId);
    java.util.Optional<OrderStatusHistory> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, OrderStatus status);
}
