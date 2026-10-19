package com.capstore.controller;

import com.capstore.dto.OrderDtos.TrackingResponse;
import com.capstore.service.OrderService;
import com.capstore.service.ReturnService;
import com.capstore.dto.ReturnDtos.ReturnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderTrackingController {
    private final OrderService orders;
    private final ReturnService returns;

    @GetMapping("/{orderId}/tracking")
    public TrackingResponse tracking(@PathVariable String orderId) {
        return orders.trackingMine(orderId);
    }

    @GetMapping("/{orderId}/returns")
    public List<ReturnResponse> returns(@PathVariable String orderId) {
        return returns.forOrderMine(orderId);
    }
}
