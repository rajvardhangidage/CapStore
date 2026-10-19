package com.capstore.repository;

import com.capstore.entity.CustomerOrder;
import com.capstore.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id and o.user.id = :userId")
    Optional<CustomerOrder> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
    Optional<CustomerOrder> findByUserIdAndCheckoutRequestKey(Long userId, String checkoutRequestKey);
    Optional<CustomerOrder> findByPublicIdAndUserId(String publicId, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.publicId = :publicId and o.user.id = :userId")
    Optional<CustomerOrder> findByPublicIdAndUserIdForUpdate(@Param("publicId") String publicId,
                                                              @Param("userId") Long userId);
    Optional<CustomerOrder> findByPublicId(String publicId);
    List<CustomerOrder> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime before);

    @Query("select o from CustomerOrder o where (:status is null or o.status = :status)")
    Page<CustomerOrder> search(@Param("status") OrderStatus status, Pageable pageable);

    @Query("""
        select coalesce(sum(o.total), 0) from CustomerOrder o
        where o.paymentStatus = com.capstore.entity.PaymentStatus.PAID and o.createdAt >= :from
        """)
    BigDecimal paidSalesSince(@Param("from") LocalDateTime from);

    @Query("select o.status, count(o) from CustomerOrder o group by o.status")
    List<Object[]> countByStatus();

    @Query("""
        select o from CustomerOrder o
        where o.paymentStatus = com.capstore.entity.PaymentStatus.PAID
          and o.createdAt between :from and :to order by o.createdAt
        """)
    List<CustomerOrder> findPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select cast(o.createdAt as date), count(o), sum(o.total) from CustomerOrder o
        where o.paymentStatus = com.capstore.entity.PaymentStatus.PAID
          and o.createdAt between :from and :to
        group by cast(o.createdAt as date) order by cast(o.createdAt as date)
        """)
    List<Object[]> salesByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        select o.user.role, count(o), sum(o.total) from CustomerOrder o
        where o.paymentStatus = com.capstore.entity.PaymentStatus.PAID
          and o.createdAt between :from and :to
        group by o.user.role
        """)
    List<Object[]> salesByCustomerType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
