package com.capstore.repository;

import com.capstore.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySlug(String slug);
    Optional<Product> findByPublicIdAndActiveTrue(String publicId);
    Optional<Product> findByPublicId(String publicId);

    @Query("""
        select p from Product p
        where p.active = true
          and (:categoryId is null or p.category.id = :categoryId)
          and (:q is null or lower(p.name) like lower(concat('%', :q, '%')))
          and (:minPrice is null or p.basePrice >= :minPrice)
          and (:maxPrice is null or p.basePrice <= :maxPrice)
        """)
    Page<Product> search(@Param("categoryId") Long categoryId,
                         @Param("q") String q,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);
}
