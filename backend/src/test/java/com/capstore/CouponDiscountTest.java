package com.capstore;

import com.capstore.entity.Coupon;
import com.capstore.entity.DiscountType;
import com.capstore.service.CouponService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponDiscountTest {
    private Coupon coupon(DiscountType type, String value, String max) {
        Coupon c = new Coupon();
        c.setDiscountType(type);
        c.setDiscountValue(new BigDecimal(value));
        c.setMaxDiscount(max == null ? null : new BigDecimal(max));
        return c;
    }

    @Test
    void percentDiscountIsCappedByMaxDiscount() {
        assertEquals(new BigDecimal("200.00"),
                CouponService.discountFor(coupon(DiscountType.PERCENT, "10", "200"), new BigDecimal("5000")));
    }

    @Test
    void percentDiscountWithoutCap() {
        assertEquals(new BigDecimal("50.00"),
                CouponService.discountFor(coupon(DiscountType.PERCENT, "10", null), new BigDecimal("500")));
    }

    @Test
    void flatDiscountCannotExceedSubtotal() {
        assertEquals(new BigDecimal("300.00"),
                CouponService.discountFor(coupon(DiscountType.FLAT, "500", null), new BigDecimal("300")));
    }
}
