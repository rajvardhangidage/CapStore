package com.capstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnProcessingScheduler {
    private final ReturnService returns;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void retryRefundsAndCloseFailedPickups() {
        returns.retryFailedRefunds();
    }
}
