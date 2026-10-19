package com.capstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CatalogDtos {
    public record CategoryRequest(@NotBlank String name, Long parentId) {}
    public record CategoryResponse(Long id, String name, String slug, Long parentId) {}

    public record VariantRequest(@NotBlank String sku, String size, String color,
                                 @DecimalMin("0.0") BigDecimal price, @Min(0) int stockQty) {}
    public record TierRequest(@Min(1) int minQty, @NotNull @DecimalMin("0.0") BigDecimal price) {}

    public record ProductRequest(
            @NotBlank String name,
            String description,
            @NotNull Long categoryId,
            @NotNull @DecimalMin("0.0") BigDecimal basePrice,
            String imageUrl,
            Boolean active,
            @Valid List<VariantRequest> variants,
            @Valid List<TierRequest> tiers,
            List<String> imageUrls,
            Boolean returnable) {}

    public record StockAdjustRequest(int delta, @NotBlank String reason) {}

    public record VariantResponse(Long id, String sku, String size, String color, BigDecimal price, int stockQty) {}
    public record TierResponse(int minQty, BigDecimal price) {}

    public record ProductResponse(Long id, String publicId, String name, String slug, String description,
                                  Long categoryId, String categoryName, BigDecimal basePrice,
                                  String imageUrl, boolean active, List<String> imageUrls,
                                  List<VariantResponse> variants, List<TierResponse> tiers,
                                  boolean returnable) {}
}
