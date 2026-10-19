package com.capstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CartDtos {
    public record AddItemRequest(@NotNull Long variantId, @Min(1) int quantity) {}
    public record UpdateItemRequest(int quantity) {}

    public record CartItemResponse(Long itemId, Long variantId, Long productId, String productPublicId, String productName,
                                   String sku, String size, String color, String imageUrl,
                                   int quantity, int availableStock, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record CartResponse(List<CartItemResponse> items, int totalQuantity, BigDecimal subtotal) {}
}
