package com.capstore.controller;

import com.capstore.dto.OrderDtos.*;
import com.capstore.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;

    public record CreatePaymentRequest(@NotNull Long orderId) {}

    @PostMapping("/create-order")
    public CreatePaymentResponse create(@Valid @RequestBody CreatePaymentRequest r) { return payments.create(r.orderId()); }

    @PostMapping("/verify")
    public OrderResponse verify(@Valid @RequestBody VerifyPaymentRequest r) { return payments.verify(r); }

    /** Public endpoint, protected by Razorpay HMAC signature. Body must be read raw. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        payments.handleWebhook(body, signature);
        return ResponseEntity.ok().build();
    }
}
