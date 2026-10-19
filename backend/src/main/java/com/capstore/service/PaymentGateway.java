package com.capstore.service;

public interface PaymentGateway {
    String createOrder(long amountPaise, String receipt);
    PaymentDetails fetchPayment(String paymentId);
    RefundDetails refund(String paymentId, long amountPaise, String idempotencyKey);

    record PaymentDetails(String orderId, long amountPaise, String currency, String status, String method) {}
    record RefundDetails(String refundId, String status) {}
}
