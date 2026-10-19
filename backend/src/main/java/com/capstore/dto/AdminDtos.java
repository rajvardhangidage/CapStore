package com.capstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminDtos {
    public record CustomerResponse(Long id, String name, String email, String phone, String role,
                                   String companyName, String gstNumber, boolean businessApproved,
                                   boolean enabled, LocalDateTime createdAt) {}

    public record LowStockItem(Long variantId, String productName, String sku, String size,
                               String color, int stockQty) {}

    public record DashboardResponse(long totalOrders, BigDecimal todaySales, BigDecimal monthSales,
                                    Map<String, Long> ordersByStatus, List<LowStockItem> lowStock) {}

    public record InventoryLogResponse(Long id, int changeQty, String reason, Long refId, LocalDateTime at) {}
}
