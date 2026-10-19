package com.capstore.service;

import com.capstore.entity.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

@Service
public class PricingService {

    /**
     * Unit price for a variant. Approved B2B accounts get the best bulk tier whose
     * min quantity is met by this line's quantity (if it is cheaper than the normal price).
     */
    public BigDecimal unitPrice(ProductVariant v, User user, int qty) {
        Product p = v.getProduct();
        BigDecimal price = v.getPrice() != null ? v.getPrice() : p.getBasePrice();
        if (user.getRole() == Role.B2B_USER && user.isBusinessApproved()) {
            Optional<BulkPriceTier> tier = p.getTiers().stream()
                    .filter(t -> t.getMinQty() <= qty)
                    .max(Comparator.comparingInt(BulkPriceTier::getMinQty));
            if (tier.isPresent() && tier.get().getPrice().compareTo(price) < 0) {
                price = tier.get().getPrice();
            }
        }
        return price;
    }
}
