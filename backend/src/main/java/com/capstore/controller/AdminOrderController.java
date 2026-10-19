package com.capstore.controller;

import com.capstore.dto.OrderDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.OrderStatus;
import com.capstore.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminService admin;

    @GetMapping
    public PageResponse<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return admin.orders(status, page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) { return admin.order(id); }

    @GetMapping("/{id}/tracking")
    public TrackingResponse tracking(@PathVariable String id) { return admin.tracking(id); }

    @PutMapping("/{id}/status")
    public OrderResponse status(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest r, Authentication a) {
        return admin.updateStatus(id, r, a.getName());
    }

    @PutMapping("/{id}/shipment")
    public OrderResponse shipment(@PathVariable String id, @Valid @RequestBody ShipmentRequest r, Authentication a) {
        return admin.setShipment(id, r, a.getName());
    }
}
