package com.capstore;

import com.capstore.entity.Product;
import com.capstore.entity.ProductVariant;
import com.capstore.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private final PricingService pricing = new PricingService();

    private ProductVariant variant() {
        Product p = new Product();
        p.setBasePrice(new BigDecimal("400"));
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        return v;
    }

    @Test
    void usesBasePriceWhenVariantHasNoPrice() {
        assertEquals(new BigDecimal("400"), pricing.unitPrice(variant()));
    }

    @Test
    void variantPriceOverridesBasePrice() {
        ProductVariant v = variant();
        v.setPrice(new BigDecimal("450"));
        assertEquals(new BigDecimal("450"), pricing.unitPrice(v));
    }
}
