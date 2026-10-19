package com.capstore.dto;

import com.capstore.entity.OrderStatus;
import com.capstore.entity.PaymentStatus;
import com.capstore.entity.ShipmentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {
    public record PlaceOrderRequest(
            @NotBlank String shipName,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits") String shipPhone,
            @NotBlank String shipLine1,
            String shipLine2,
            @NotBlank String shipCity,
            @NotBlank String shipState,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits") String shipPincode,
            @NotBlank @Size(max = 100) String checkoutRequestKey,
            String couponCode) {}

    public record OrderItemResponse(Long id, String productName, String sku, String size, String color,
                                    int quantity, BigDecimal unitPrice, BigDecimal lineTotal,
                                    boolean returnable) {}

    public record ShippingAddress(String name, String phone, String line1, String line2,
                                  String city, String state, String pincode) {}

    public record OrderResponse(Long id, String publicId, String orderNo, OrderStatus status, PaymentStatus paymentStatus,
                                BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                                BigDecimal shipping, BigDecimal total, ShippingAddress address,
                                String courier, String trackingNo, String trackingUrl,
                                String customerName, String customerEmail,
                                List<OrderItemResponse> items, LocalDateTime createdAt, String couponCode) {}

    public record TimelineEntry(OrderStatus status, String note, LocalDateTime at) {}

    public record UpdateStatusRequest(@NotNull OrderStatus status, String note) {}
    public record ShipmentRequest(@NotBlank @Size(max = 100) String courier,
                                  @NotBlank @Size(max = 100) String trackingNo,
                                  @Size(max = 500) String trackingUrl, LocalDate expectedDelivery) {}

    public record TrackingWebhookRequest(@NotBlank @Size(max = 190) String eventId,
                                        @NotBlank @Size(max = 100) String awb,
                                        @NotBlank @Size(max = 50) String status,
                                        @Size(max = 500) String description,
                                        LocalDate expectedDelivery, LocalDateTime occurredAt) {}

    public record TrackingEventResponse(String courier, String awb, ShipmentStatus status, String description,
                                        LocalDateTime occurredAt) {}

    public record TrackingResponse(Long orderId, ShipmentStatus status, LocalDate expectedDelivery,
                                   List<TrackingEventResponse> events) {}

    // ----- payments -----
    public record CreatePaymentResponse(String keyId, String gatewayOrderId, long amountPaise,
                                        String currency, String orderNo) {}

    public record VerifyPaymentRequest(
            @NotNull Long orderId,
            @NotBlank @JsonProperty("razorpay_order_id") String razorpayOrderId,
            @NotBlank @JsonProperty("razorpay_payment_id") String razorpayPaymentId,
            @NotBlank @JsonProperty("razorpay_signature") String razorpaySignature) {}
}
