package com.capstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {
    private final OrderService orderService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void cancelUnpaidOrders() {
        int n = orderService.cancelExpired();
        if (n > 0) log.info("Auto-cancelled {} unpaid orders", n);
    }
}
