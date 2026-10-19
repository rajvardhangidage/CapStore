package com.capstore.repository;

import com.capstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    java.util.Optional<OrderItem> findByIdAndOrderId(Long id, Long orderId);

    @Query("""
        select count(i) from OrderItem i
        where i.order.user.id = :userId and i.variant.product.id = :productId
          and i.order.paymentStatus = com.capstore.entity.PaymentStatus.PAID
        """)
    long countPurchases(@Param("userId") Long userId, @Param("productId") Long productId);

    default boolean hasPurchased(Long userId, Long productId) { return countPurchases(userId, productId) > 0; }

    @Query("""
        select i.productName, sum(i.quantity), sum(i.lineTotal) from OrderItem i
        where i.order.paymentStatus = com.capstore.entity.PaymentStatus.PAID
          and i.order.createdAt between :from and :to
        group by i.productName order by sum(i.lineTotal) desc
        """)
    List<Object[]> productSales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select c.name, sum(i.quantity), sum(i.lineTotal) from OrderItem i
        join i.variant v join v.product p join p.category c
        where i.order.paymentStatus = com.capstore.entity.PaymentStatus.PAID
          and i.order.createdAt between :from and :to
        group by c.name order by sum(i.lineTotal) desc
        """)
    List<Object[]> categorySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
