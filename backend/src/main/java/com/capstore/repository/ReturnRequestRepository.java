package com.capstore.repository;

import com.capstore.entity.ReturnRequest;
import com.capstore.entity.ReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByPublicIdAndOrderUserId(String publicId, Long userId);
    List<ReturnRequest> findByOrderIdAndOrderUserIdOrderByCreatedAtDesc(Long orderId, Long userId);
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
    List<ReturnRequest> findByOrderUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByOrderItemIdAndStatusIn(Long orderItemId, Collection<ReturnStatus> statuses);

    @Query("select coalesce(sum(r.quantity), 0) from ReturnRequest r where r.orderItem.id = :itemId and r.completedReturn = true")
    int sumCompletedQuantity(@Param("itemId") Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReturnRequest r where r.publicId = :publicId")
    Optional<ReturnRequest> findByPublicIdForUpdate(@Param("publicId") String publicId);

    List<ReturnRequest> findByStatusAndPickupFailuresGreaterThanEqual(ReturnStatus status, int failures);
    Optional<ReturnRequest> findByReplacementOrderId(Long replacementOrderId);
}
