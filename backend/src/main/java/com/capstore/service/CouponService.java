package com.capstore.service;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.entity.Coupon;
import com.capstore.entity.DiscountType;
import com.capstore.exception.ApiException;
import com.capstore.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository repo;

    public Coupon validate(String code, BigDecimal subtotal) {
        Coupon c = repo.findByCodeIgnoreCase(code.trim()).orElseThrow(() -> ApiException.badRequest("Invalid coupon code"));
        if (!c.isActive()) throw ApiException.badRequest("This coupon is not active");
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(LocalDateTime.now()))
            throw ApiException.badRequest("This coupon has expired");
        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit())
            throw ApiException.badRequest("This coupon has reached its usage limit");
        if (c.getMinOrder() != null && subtotal.compareTo(c.getMinOrder()) < 0)
            throw ApiException.badRequest("Add items worth Rs. " + c.getMinOrder().stripTrailingZeros().toPlainString() + " or more to use this coupon");
        return c;
    }

    public static BigDecimal discountFor(Coupon c, BigDecimal subtotal) {
        BigDecimal d = c.getDiscountType() == DiscountType.PERCENT
                ? subtotal.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.getDiscountValue();
        if (c.getMaxDiscount() != null && d.compareTo(c.getMaxDiscount()) > 0) d = c.getMaxDiscount();
        if (d.compareTo(subtotal) > 0) d = subtotal;
        return d.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public CouponValidation preview(String code, BigDecimal subtotal) {
        Coupon c = validate(code, subtotal);
        return new CouponValidation(c.getCode(), discountFor(c, subtotal), c.getDescription());
    }

    @Transactional(readOnly = true)
    public List<CouponOffer> available(BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        return repo.findByActiveTrueOrderByIdDesc().stream()
                .filter(c -> c.getExpiresAt() == null || !c.getExpiresAt().isBefore(now))
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
                .filter(c -> c.getMinOrder() == null || subtotal.compareTo(c.getMinOrder()) >= 0)
                .map(c -> new CouponOffer(c.getCode(), c.getDescription(), c.getDiscountType(),
                        c.getDiscountValue(), c.getMinOrder(), c.getMaxDiscount(), c.getExpiresAt()))
                .toList();
    }

    public int consume(Long id) { return repo.consume(id); }
    public void release(String code) { repo.release(code); }

    // ----- admin -----
    @Transactional(readOnly = true)
    public List<CouponResponse> list() { return repo.findAll(Sort.by("id").descending()).stream().map(this::toResponse).toList(); }

    @Transactional
    public CouponResponse create(CouponRequest r) {
        String code = r.code().trim().toUpperCase();
        if (repo.existsByCodeIgnoreCase(code)) throw ApiException.conflict("Coupon code already exists");
        Coupon c = new Coupon();
        c.setCode(code);
        apply(c, r);
        return toResponse(repo.save(c));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest r) {
        Coupon c = repo.findById(id).orElseThrow(() -> ApiException.notFound("Coupon not found"));
        apply(c, r);
        return toResponse(c);
    }

    private void apply(Coupon c, CouponRequest r) {
        if (r.discountType() == DiscountType.PERCENT && r.discountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw ApiException.badRequest("Percentage cannot exceed 100");
        c.setDescription(r.description());
        c.setDiscountType(r.discountType());
        c.setDiscountValue(r.discountValue());
        c.setMinOrder(r.minOrder() == null ? BigDecimal.ZERO : r.minOrder());
        c.setMaxDiscount(r.maxDiscount());
        c.setExpiresAt(r.expiresAt());
        c.setUsageLimit(r.usageLimit());
        if (r.active() != null) c.setActive(r.active());
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinOrder(), c.getMaxDiscount(), c.getExpiresAt(), c.getUsageLimit(), c.getUsedCount(), c.isActive());
    }
}
