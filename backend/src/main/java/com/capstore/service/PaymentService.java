package com.capstore.service;

import com.capstore.dto.OrderDtos.*;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderRepository;
import com.capstore.repository.PaymentRepository;
import com.capstore.security.AuthFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final TrackingService tracking;
    private final AuthFacade auth;
    private final ObjectMapper mapper;
    private final PaymentGateway gateway;
    private final ReturnService returns;

    @Value("${app.razorpay.dev-mode}") private boolean devMode;
    @Value("${app.razorpay.key-id}") private String keyId;
    @Value("${app.razorpay.key-secret}") private String keySecret;
    @Value("${app.razorpay.webhook-secret}") private String webhookSecret;

    /** Step 1: create (or reuse) a gateway order for an unpaid order. */
    @Transactional
    public CreatePaymentResponse create(Long orderId) {
        User u = auth.currentUser();
        CustomerOrder o = orders.findByIdAndUserIdForUpdate(orderId, u.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT)
            throw ApiException.badRequest("Order is not awaiting payment");

        long paise = o.getTotal().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        var existing = payments.findTopByOrderIdAndStatusOrderByIdDesc(orderId, PaymentStatus.PENDING);
        String gatewayOrderId;
        boolean existingMatchesMode = existing
                .map(payment -> devMode == payment.getGatewayOrderId().startsWith("order_DEV_"))
                .orElse(false);
        if (existingMatchesMode) {
            gatewayOrderId = existing.get().getGatewayOrderId();
        } else {
            gatewayOrderId = devMode ? "order_DEV_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14)
                                     : gateway.createOrder(paise, o.getOrderNo());
            Payment p = existing.orElseGet(Payment::new);
            p.setOrder(o);
            p.setGatewayOrderId(gatewayOrderId);
            p.setAmount(o.getTotal());
            p.setStatus(PaymentStatus.PENDING);
            payments.save(p);
        }
        return new CreatePaymentResponse(devMode ? "dev" : keyId, gatewayOrderId, paise, "INR", o.getOrderNo());
    }

    /** Step 2: browser reports success -> verify signature on the server. */
    @Transactional
    public OrderResponse verify(VerifyPaymentRequest r) {
        User u = auth.currentUser();
        Payment p = payments.findByGatewayOrderIdForUpdate(r.razorpayOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        CustomerOrder o = p.getOrder();
        if (!o.getId().equals(r.orderId()) || !o.getUser().getId().equals(u.getId()))
            throw ApiException.notFound("Payment not found");

        boolean valid = devMode
                ? "dev".equals(r.razorpaySignature())
                : safeEquals(hmacHex(r.razorpayOrderId() + "|" + r.razorpayPaymentId(), keySecret), r.razorpaySignature());
        if (!valid) throw ApiException.badRequest("Payment verification failed");
        if (!devMode) {
            PaymentGateway.PaymentDetails details = gateway.fetchPayment(r.razorpayPaymentId());
            long expectedPaise = p.getAmount().movePointRight(2).longValueExact();
            if (!r.razorpayOrderId().equals(details.orderId()) || expectedPaise != details.amountPaise()
                    || !"INR".equals(details.currency()) || !"captured".equalsIgnoreCase(details.status()))
                throw ApiException.badRequest("Payment is not captured for the expected order amount");
        }

        markPaid(p, r.razorpayPaymentId(), null);
        return orderService.toResponse(o);
    }

    /** Safety net: Razorpay server-to-server webhook (works even if user closes the browser). */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank())
            throw ApiException.badRequest("Webhook not configured");
        if (signature == null || !safeEquals(hmacHex(rawBody, webhookSecret), signature))
            throw ApiException.badRequest("Invalid webhook signature");
        try {
            JsonNode root = mapper.readTree(rawBody);
            String event = root.path("event").asText();
            if (event.equals("refund.processed") || event.equals("refund.failed")) {
                JsonNode refund = root.path("payload").path("refund").path("entity");
                String refundId = refund.path("id").asText(null);
                if (refundId == null || refundId.isBlank()) {
                    log.warn("Ignoring Razorpay {} webhook without refund id", event);
                    return;
                }
                if (event.equals("refund.processed"))
                    returns.confirmRefund(refundId, refund.path("payment_id").asText(null),
                            refund.path("amount").asLong(-1));
                else returns.failRefund(refundId);
            } else if (event.equals("payment.captured") || event.equals("order.paid") || event.equals("payment.failed")) {
                JsonNode payload = root.path("payload");
                JsonNode payment = payload.path("payment").path("entity");
                JsonNode order = payload.path("order").path("entity");
                String gatewayOrderId = payment.path("order_id").asText(null);
                if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
                    gatewayOrderId = order.path("id").asText(null);
                }
                if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
                    log.warn("Ignoring Razorpay {} webhook without an order id", event);
                    return;
                }

                String matchedGatewayOrderId = gatewayOrderId;
                String paymentId = payment.path("id").asText(null);
                String method = payment.path("method").asText(null);
                payments.findByGatewayOrderIdForUpdate(matchedGatewayOrderId).ifPresentOrElse(p -> {
                    if (event.equals("payment.failed")) {
                        if (p.getStatus() == PaymentStatus.PENDING) p.setStatus(PaymentStatus.FAILED);
                        return;
                    }
                    long actualAmount = event.equals("payment.captured")
                            ? payment.path("amount").asLong(-1)
                            : order.path("amount_paid").asLong(-1);
                    String currency = event.equals("payment.captured")
                            ? payment.path("currency").asText(null)
                            : order.path("currency").asText(null);
                    String payloadOrderId = payment.path("order_id").asText(null);
                    if (event.equals("payment.captured") && !"captured".equalsIgnoreCase(payment.path("status").asText()))
                        throw ApiException.badRequest("Payment webhook is not for a captured payment");
                    long expectedAmount = p.getAmount().movePointRight(2).longValueExact();
                    if (actualAmount != expectedAmount || !"INR".equals(currency)
                            || (payloadOrderId != null && !payloadOrderId.equals(p.getGatewayOrderId()))) {
                        log.error("Rejecting mismatched Razorpay payment for local gateway order {}", matchedGatewayOrderId);
                        throw ApiException.badRequest("Payment webhook amount, currency, or order does not match");
                    }
                    markPaid(p, paymentId, method);
                }, () -> log.warn("No local payment found for Razorpay order {}", matchedGatewayOrderId));
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not process webhook", ex);
            throw ApiException.badRequest("Malformed webhook payload");
        }
    }

    /** Idempotent: safe to call from both verify() and the webhook. */
    private void markPaid(Payment p, String paymentId, String method) {
        CustomerOrder o = p.getOrder();
        if (p.getStatus() == PaymentStatus.PAID) {
            if (o.getPaymentStatus() != PaymentStatus.REFUND_PENDING
                    && paymentId != null && p.getGatewayPaymentId() != null
                    && !paymentId.equals(p.getGatewayPaymentId())) {
                o.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                orderService.addHistory(o.getId(), o.getStatus(),
                        "Additional payment received; refund required for payment " + paymentId, "system");
            }
            return;
        }
        boolean alreadyPaid = o.getPaymentStatus() == PaymentStatus.PAID;
        p.setStatus(PaymentStatus.PAID);
        p.setGatewayPaymentId(paymentId);
        if (method != null) p.setMethod(method);

        if (o.getStatus() == OrderStatus.PENDING_PAYMENT) {
            o.setPaymentStatus(PaymentStatus.PAID);
            o.setStatus(OrderStatus.CONFIRMED);
            orderService.removePurchasedItemsFromCart(o);
            orderService.addHistory(o.getId(), OrderStatus.CONFIRMED, "Payment received", "system");
            tracking.paymentConfirmed(o);
        } else if (o.getStatus() == OrderStatus.CANCELLED) {
            // customer paid after the order was auto-cancelled -> needs a manual refund
            o.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            orderService.addHistory(o.getId(), OrderStatus.CANCELLED,
                    "Payment received after cancellation - refund required", "system");
        } else if (alreadyPaid) {
            o.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            orderService.addHistory(o.getId(), o.getStatus(),
                    "Additional payment received; refund required for payment " + paymentId, "system");
        }
    }

    private String hmacHex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private boolean safeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
