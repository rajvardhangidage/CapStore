package com.capstore.controller;

import com.capstore.dto.CartDtos.CartResponse;
import com.capstore.dto.ExtraDtos.*;
import com.capstore.service.BannerService;
import com.capstore.service.CartService;
import com.capstore.service.CouponService;
import com.capstore.service.StoreSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public storefront data + coupon preview. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StoreController {
    private final BannerService banners;
    private final CouponService coupons;
    private final CartService cart;
    private final StoreSettingsService storeSettings;

    @GetMapping("/banners")
    public List<BannerResponse> banners() { return banners.active(); }

    @GetMapping("/config")
    public StoreConfig config() { return storeSettings.get(); }

    @PostMapping("/coupons/validate")
    public CouponValidation validate(@Valid @RequestBody ValidateCouponRequest r) {
        CartResponse c = cart.get();
        return coupons.preview(r.code(), c.subtotal());
    }

    @GetMapping("/coupons/available")
    public List<CouponOffer> availableCoupons() {
        CartResponse c = cart.get();
        return coupons.available(c.subtotal());
    }
}
