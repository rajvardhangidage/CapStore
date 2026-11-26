package com.capstore.service;

import com.capstore.entity.ProductVariant;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

    /** Unit price for a variant: its own price if set, otherwise the product's base price. */
    public BigDecimal unitPrice(ProductVariant v) {
        return v.getPrice() != null ? v.getPrice() : v.getProduct().getBasePrice();
    }
}
