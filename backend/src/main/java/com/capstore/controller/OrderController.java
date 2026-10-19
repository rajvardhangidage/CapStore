package com.capstore.controller;

import com.capstore.dto.OrderDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody PlaceOrderRequest r) { return orders.place(r); }

    @GetMapping
    public PageResponse<OrderResponse> mine(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return orders.myOrders(page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) { return orders.getMine(id); }

    @GetMapping("/{id}/tracking")
    public TrackingResponse tracking(@PathVariable String id) { return orders.trackingMine(id); }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable String id) { return orders.cancelMine(id); }
}
