package com.capstore.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class RazorpayPaymentGateway implements PaymentGateway {
    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentGateway(@Value("${app.razorpay.key-id}") String keyId,
                                  @Value("${app.razorpay.key-secret}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public String createOrder(long amountPaise, String receipt) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", "INR");
            request.put("receipt", receipt);
            return client().orders.create(request).get("id");
        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed", ex);
            throw new com.capstore.exception.ApiException(HttpStatus.BAD_GATEWAY, "Payment gateway error, please try again");
        }
    }

    @Override
    public PaymentDetails fetchPayment(String paymentId) {
        try {
            JSONObject payment = client().payments.fetch(paymentId).toJson();
            return new PaymentDetails(payment.optString("order_id", null), payment.optLong("amount", -1),
                    payment.optString("currency", null), payment.optString("status", null),
                    payment.optString("method", null));
        } catch (RazorpayException ex) {
            log.error("Razorpay payment lookup failed", ex);
            throw new com.capstore.exception.ApiException(HttpStatus.BAD_GATEWAY, "Could not verify payment with gateway");
        }
    }

    @Override
    public RefundDetails refund(String paymentId, long amountPaise, String idempotencyKey) {
        try {
            JSONObject body = new JSONObject();
            body.put("amount", amountPaise);
            String credentials = Base64.getEncoder().encodeToString(
                    (keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/payments/" + paymentId + "/refund"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .header("X-Razorpay-Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Razorpay refund request returned HTTP " + response.statusCode());
            JSONObject result = new JSONObject(response.body());
            return new RefundDetails(result.optString("id", null), result.optString("status", null));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new com.capstore.exception.ApiException(HttpStatus.BAD_GATEWAY, "Refund request was interrupted");
        } catch (Exception ex) {
            log.error("Razorpay refund request failed: {}", ex.getMessage());
            throw new com.capstore.exception.ApiException(HttpStatus.BAD_GATEWAY, "Refund request failed; it may be retried safely");
        }
    }

    private RazorpayClient client() throws RazorpayException {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank())
            throw new com.capstore.exception.ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payment gateway is not configured");
        return new RazorpayClient(keyId, keySecret);
    }
}
