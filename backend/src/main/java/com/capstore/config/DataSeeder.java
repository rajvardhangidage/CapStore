package com.capstore.config;

import com.capstore.dto.CatalogDtos.*;
import com.capstore.entity.Role;
import com.capstore.entity.User;
import com.capstore.entity.Banner;
import com.capstore.entity.Coupon;
import com.capstore.entity.DiscountType;
import com.capstore.repository.BannerRepository;
import com.capstore.repository.CategoryRepository;
import com.capstore.repository.CouponRepository;
import com.capstore.repository.UserRepository;
import com.capstore.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Creates the first admin and some sample caps so you can click around immediately. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final CatalogService catalog;
    private final CouponRepository couponRepo;
    private final BannerRepository bannerRepo;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email}") private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;

    @Override
    public void run(String... args) {
        if (!users.existsByEmail(adminEmail)) {
            User a = new User();
            a.setName("Store Admin");
            a.setEmail(adminEmail);
            a.setPhone("9999999999");
            a.setPasswordHash(encoder.encode(adminPassword));
            a.setRole(Role.ADMIN);
            users.save(a);
            log.info("Created admin user {}", adminEmail);
        }
        if (categories.count() == 0) {
            Long baseball = catalog.createCategory(new CategoryRequest("Baseball Caps", null)).id();
            Long snapback = catalog.createCategory(new CategoryRequest("Snapbacks", null)).id();
            catalog.createCategory(new CategoryRequest("Bucket Hats", null));

            catalog.create(new ProductRequest("Classic Cotton Baseball Cap", "Soft 100% cotton, curved brim, adjustable strap.",
                    baseball, new BigDecimal("399"), null, true,
                    List.of(new VariantRequest("CBC-BLK-M", "M", "Black", null, 50),
                            new VariantRequest("CBC-BLK-L", "L", "Black", null, 40),
                            new VariantRequest("CBC-NVY-M", "M", "Navy", null, 30)),
                    List.of(new TierRequest(25, new BigDecimal("349")), new TierRequest(100, new BigDecimal("299"))), null, null));

            catalog.create(new ProductRequest("Urban Flat-Brim Snapback", "Structured flat brim with snap closure.",
                    snapback, new BigDecimal("549"), null, true,
                    List.of(new VariantRequest("SNP-WHT-F", "Free", "White", null, 25),
                            new VariantRequest("SNP-RED-F", "Free", "Red", null, 5)),
                    List.of(new TierRequest(50, new BigDecimal("479"))), null, null));

            Coupon c = new Coupon();
            c.setCode("WELCOME10");
            c.setDescription("10% off your first order (up to Rs. 200)");
            c.setDiscountType(DiscountType.PERCENT);
            c.setDiscountValue(new BigDecimal("10"));
            c.setMinOrder(new BigDecimal("500"));
            c.setMaxDiscount(new BigDecimal("200"));
            couponRepo.save(c);

            Banner b = new Banner();
            b.setTitle("Fresh drops, straight from our workshop");
            b.setSubtitle("Cotton caps, snapbacks and bucket hats. Order one or order a hundred.");
            b.setLinkUrl("/products");
            b.setSortOrder(1);
            bannerRepo.save(b);
            log.info("Seeded sample catalog");
        }
    }
}
