package com.capstore.repository;

import com.capstore.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByIdDesc(Long productId);
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
}
