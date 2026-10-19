package com.capstore.service;

public record OrderStatusMailEvent(Long orderId, String type, String recipient, String subject, String body) {}
