package com.capstore.repository;

import com.capstore.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Coupon> findByActiveTrueOrderByIdDesc();

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)")
    int consume(@Param("id") Long id);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount - 1 where c.code = :code and c.usedCount > 0")
    int release(@Param("code") String code);
}
