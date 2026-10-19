package com.capstore.repository;

import com.capstore.entity.QuoteRequest;
import com.capstore.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    List<QuoteRequest> findByUserIdOrderByIdDesc(Long userId);

    @Query("select q from QuoteRequest q where (:status is null or q.status = :status)")
    Page<QuoteRequest> search(@Param("status") QuoteStatus status, Pageable pageable);
}
