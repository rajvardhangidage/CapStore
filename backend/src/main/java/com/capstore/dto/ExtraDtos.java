package com.capstore.dto;

import com.capstore.entity.DiscountType;
import com.capstore.entity.QuoteStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExtraDtos {
    // ----- password -----
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword) {}
    public record ContactInquiryRequest(@NotBlank @Size(max = 100) @Pattern(regexp = "^[^\\r\\n]+$") String name,
                                        @NotBlank @Email @Size(max = 254) @Pattern(regexp = "^[^\\r\\n]+$") String email,
                                        @NotBlank @Pattern(regexp = "^[+]?[0-9() .-]{7,24}$") String phone,
                                        @NotBlank @Size(min = 10, max = 3000) String message) {}
    public record CreateAdminRequest(@NotBlank String name, @NotBlank @Email String email,
                                     @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits") String phone,
                                     @NotBlank @Size(min = 8) String password) {}

    // ----- addresses -----
    public record AddressRequest(String label, @NotBlank String name,
                                 @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits") String phone,
                                 @NotBlank String line1, String line2, @NotBlank String city, @NotBlank String state,
                                 @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits") String pincode,
                                 boolean defaultAddress) {}
    public record AddressResponse(Long id, String label, String name, String phone, String line1, String line2,
                                  String city, String state, String pincode, boolean defaultAddress) {}

    // ----- coupons -----
    public record CouponRequest(@NotBlank String code, String description, @NotNull DiscountType discountType,
                                @NotNull @DecimalMin("0.01") BigDecimal discountValue, BigDecimal minOrder,
                                BigDecimal maxDiscount, LocalDateTime expiresAt, Integer usageLimit, Boolean active) {}
    public record CouponResponse(Long id, String code, String description, DiscountType discountType,
                                 BigDecimal discountValue, BigDecimal minOrder, BigDecimal maxDiscount,
                                 LocalDateTime expiresAt, Integer usageLimit, int usedCount, boolean active) {}
    public record CouponOffer(String code, String description, DiscountType discountType,
                              BigDecimal discountValue, BigDecimal minOrder, BigDecimal maxDiscount,
                              LocalDateTime expiresAt) {}
    public record ValidateCouponRequest(@NotBlank String code) {}
    public record CouponValidation(String code, BigDecimal discount, String description) {}

    // ----- quotes -----
    public record QuoteCreateRequest(Long productId, @Min(1) int quantity, @NotBlank String description, String logoUrl) {}
    public record QuoteRespondRequest(@NotNull @DecimalMin("0.01") BigDecimal quotedPrice, String adminNote) {}
    public record QuoteDecisionRequest(boolean accept) {}
    public record QuoteResponse(Long id, Long productId, String productName, int quantity, String description,
                                String logoUrl, QuoteStatus status, BigDecimal quotedPrice, String adminNote,
                                String customerName, String customerEmail, String companyName,
                                LocalDateTime createdAt) {}

    // ----- reviews -----
    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String body) {}
    public record ReviewResponse(Long id, String userName, int rating, String body, LocalDateTime createdAt) {}
    public record ReviewSummary(double average, long count, boolean canReview, List<ReviewResponse> reviews) {}

    // ----- banners / config -----
    public record BannerRequest(@NotBlank String title, String subtitle, String imageUrl, String linkUrl,
                                int sortOrder, Boolean active) {}
    public record BannerResponse(Long id, String title, String subtitle, String imageUrl, String linkUrl,
                                 int sortOrder, boolean active) {}
    public record StoreConfig(String storeName, BigDecimal gstRatePercent, BigDecimal shippingFlatRate,
                              BigDecimal freeShippingAbove) {}
    public record StoreSettingsRequest(@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal gstRatePercent,
                                       @NotNull @DecimalMin("0") BigDecimal shippingFlatRate,
                                       @NotNull @DecimalMin("0") BigDecimal freeShippingAbove) {}

    // ----- reports -----
    public record ReportData(String title, List<String> headers, List<List<Object>> rows) {}
}
