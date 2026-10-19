package com.capstore.service;

import com.capstore.dto.AdminDtos.*;
import com.capstore.dto.OrderDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final OrderRepository orders;
    private final OrderService orderService;
    private final TrackingService tracking;
    private final UserRepository users;
    private final ProductVariantRepository variants;
    private final InventoryLogRepository inventoryLogs;
    private final NotificationService notifier;

    @Value("${app.inventory.low-stock-threshold}") private int lowStockThreshold;

    // ---------- orders ----------
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> orders(OrderStatus status, int page, int size) {
        var p = orders.search(status, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending()));
        return PageResponse.of(p, orderService::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse order(String publicId) { return orderService.toResponse(findByPublicId(publicId)); }

    @Transactional(readOnly = true)
    public TrackingResponse tracking(String publicId) { return tracking.tracking(findByPublicId(publicId)); }

    @Transactional
    public OrderResponse updateStatus(String publicId, UpdateStatusRequest r, String adminEmail) {
        CustomerOrder o = findByPublicId(publicId);
        OrderStatus ns = r.status();
        if (ns == OrderStatus.PENDING_PAYMENT) throw ApiException.badRequest("Invalid status");
        if (o.getStatus() == OrderStatus.CANCELLED) throw ApiException.badRequest("Order is already cancelled");

        if (ns == OrderStatus.CANCELLED) {
            orderService.cancelInternal(o, noteOr(r.note(), "Cancelled by admin"), adminEmail);
            notifier.statusChanged(o, r.note());
            return orderService.toResponse(o);
        }
        if (o.getPaymentStatus() != PaymentStatus.PAID && o.getPaymentStatus() != PaymentStatus.REFUND_PENDING)
            throw ApiException.badRequest("Order has not been paid yet");
        if (ns == OrderStatus.SHIPPED || ns == OrderStatus.OUT_FOR_DELIVERY || ns == OrderStatus.DELIVERED) {
            tracking.updateFromAdmin(o, ns, adminEmail);
            return orderService.toResponse(o);
        }
        if (ns == OrderStatus.REFUNDED) o.setPaymentStatus(PaymentStatus.REFUNDED);
        o.setStatus(ns);
        orderService.addHistory(o.getId(), ns, noteOr(r.note(), "Status updated"), adminEmail);
        notifier.statusChanged(o, r.note());
        return orderService.toResponse(o);
    }

    @Transactional
    public OrderResponse setShipment(String publicId, ShipmentRequest r, String adminEmail) {
        CustomerOrder o = findByPublicId(publicId);
        if (o.getStatus() == OrderStatus.CANCELLED || o.getPaymentStatus() != PaymentStatus.PAID)
            throw ApiException.badRequest("Order cannot be shipped");
        tracking.createShipment(o, r, adminEmail);
        return orderService.toResponse(o);
    }

    // ---------- customers ----------
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> customers(Role role, int page, int size) {
        var p = users.search(role, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending()));
        return PageResponse.of(p, this::toResponse);
    }

    @Transactional
    public CustomerResponse approveBusiness(Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getRole() != Role.B2B_USER) throw ApiException.badRequest("Not a business account");
        u.setBusinessApproved(true);
        notifier.businessApproved(u);
        return toResponse(u);
    }

    @Transactional
    public CustomerResponse setEnabled(Long id, boolean enabled) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getRole() == Role.ADMIN) throw ApiException.badRequest("Cannot change an admin account");
        u.setEnabled(enabled);
        return toResponse(u);
    }

    // ---------- dashboard / inventory ----------
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) byStatus.put(s.name(), 0L);
        long total = 0;
        for (Object[] row : orders.countByStatus()) {
            long c = ((Number) row[1]).longValue();
            byStatus.put(((OrderStatus) row[0]).name(), c);
            total += c;
        }
        BigDecimal today = orders.paidSalesSince(LocalDate.now().atStartOfDay());
        BigDecimal month = orders.paidSalesSince(LocalDate.now().withDayOfMonth(1).atStartOfDay());
        return new DashboardResponse(total, today, month, byStatus, lowStock());
    }

    @Transactional(readOnly = true)
    public List<LowStockItem> lowStock() {
        return variants.findLowStock(lowStockThreshold).stream().map(v -> new LowStockItem(
                v.getId(), v.getProduct().getName(), v.getSku(), v.getSize(), v.getColor(), v.getStockQty())).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryLogResponse> inventoryLog(Long variantId, int page, int size) {
        var p = inventoryLogs.findByVariantIdOrderByIdDesc(variantId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.of(p, l -> new InventoryLogResponse(l.getId(), l.getChangeQty(), l.getReason(), l.getRefId(), l.getCreatedAt()));
    }

    // ---------- helpers ----------
    private CustomerOrder find(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    private CustomerOrder findByPublicId(String publicId) {
        return orders.findByPublicId(publicId).orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    private String noteOr(String note, String fallback) { return (note == null || note.isBlank()) ? fallback : note.trim(); }

    private CustomerResponse toResponse(User u) {
        return new CustomerResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole().name(),
                u.getCompanyName(), u.getGstNumber(), u.isBusinessApproved(), u.isEnabled(), u.getCreatedAt());
    }
}
