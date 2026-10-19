package com.capstore.service;

import java.time.LocalDate;

public record OrderNotificationEvent(Long orderId, String type, String recipient, String customerName,
                                     String orderNo, String awb, String courier, String trackingUrl,
                                     String status, LocalDate expectedDelivery) {}
