package com.capstore.controller;

import com.capstore.dto.ReturnDtos.ReturnResponse;
import com.capstore.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class VersionedOrderReturnsController {
    private final ReturnService returns;

    @GetMapping("/{orderId}/returns")
    public List<ReturnResponse> forOrder(@PathVariable String orderId) {
        return returns.forOrderMine(orderId);
    }
}
