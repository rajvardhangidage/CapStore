package com.capstore;

import com.capstore.entity.*;
import com.capstore.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private final PricingService pricing = new PricingService();

    private ProductVariant variant() {
        Product p = new Product();
        p.setBasePrice(new BigDecimal("400"));
        BulkPriceTier t1 = new BulkPriceTier(); t1.setMinQty(25); t1.setPrice(new BigDecimal("350")); t1.setProduct(p);
        BulkPriceTier t2 = new BulkPriceTier(); t2.setMinQty(100); t2.setPrice(new BigDecimal("300")); t2.setProduct(p);
        p.getTiers().add(t1);
        p.getTiers().add(t2);
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        return v;
    }

    private User user(Role role, boolean approved) {
        User u = new User();
        u.setRole(role);
        u.setBusinessApproved(approved);
        return u;
    }

    @Test
    void retailCustomerPaysBasePriceEvenForLargeQuantities() {
        assertEquals(new BigDecimal("400"), pricing.unitPrice(variant(), user(Role.USER, false), 200));
    }

    @Test
    void approvedBusinessGetsBestMatchingTier() {
        User b2b = user(Role.B2B_USER, true);
        assertEquals(new BigDecimal("400"), pricing.unitPrice(variant(), b2b, 10));
        assertEquals(new BigDecimal("350"), pricing.unitPrice(variant(), b2b, 30));
        assertEquals(new BigDecimal("300"), pricing.unitPrice(variant(), b2b, 100));
    }

    @Test
    void unapprovedBusinessPaysBasePrice() {
        assertEquals(new BigDecimal("400"), pricing.unitPrice(variant(), user(Role.B2B_USER, false), 100));
    }

    @Test
    void variantPriceOverridesBasePrice() {
        ProductVariant v = variant();
        v.setPrice(new BigDecimal("450"));
        assertEquals(new BigDecimal("450"), pricing.unitPrice(v, user(Role.USER, false), 1));
    }
}
