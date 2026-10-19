package com.capstore.service;

import com.capstore.dto.OrderDtos.*;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderStatusHistoryRepository;
import com.capstore.repository.ShipmentRepository;
import com.capstore.repository.TrackingEventRepository;
import com.capstore.repository.ReturnRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.EnumSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {
    private final ShipmentRepository shipments;
    private final TrackingEventRepository trackingEvents;
    private final ReturnRequestRepository returnRequests;
    private final OrderStatusHistoryRepository orderHistory;
    private final CourierClient courierClient;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    @Value("${app.courier-webhook-secret:}") private String webhookSecret;
    @Value("${app.frontend-url}") private String frontendUrl;

    public void paymentConfirmed(CustomerOrder order) {
        events.publishEvent(new OrderNotificationEvent(order.getId(), "ORDER_CONFIRMED",
                order.getUser().getEmail(), order.getUser().getName(), order.getOrderNo(),
                null, null, frontendUrl + "/orders/" + order.getPublicId(), null, null));
    }

    @Transactional
    public void createShipment(CustomerOrder order, ShipmentRequest request, String updatedBy) {
        String courier = normalizeCourier(request.courier());
        String awb = request.trackingNo().trim();
        Shipment shipment = shipments.findByOrderIdForUpdate(order.getId()).orElseGet(() -> {
            Shipment created = new Shipment();
            created.setOrder(order);
            created.setDirection(ShipmentDirection.FORWARD);
            created.setCourierName(courier);
            created.setAwb(awb);
            return shipments.save(created);
        });
        if (!shipment.getAwb().equals(awb) || !shipment.getCourierName().equals(courier))
            throw ApiException.conflict("A shipment already exists with a different courier or AWB");
        shipment.setTrackingUrl(request.trackingUrl());
        shipment.setExpectedDelivery(request.expectedDelivery());

        recordEvent(shipment, "admin-created-" + awb, ShipmentStatus.CREATED, "Shipment created", LocalDateTime.now());
        EventResult shipped = recordEvent(shipment, "admin-shipped-" + awb, ShipmentStatus.SHIPPED,
                "Shipment handed to " + request.courier().trim(), LocalDateTime.now());

        order.setCourier(request.courier().trim());
        order.setTrackingNo(awb);
        order.setTrackingUrl(request.trackingUrl());
        if (SHIPPABLE.contains(order.getStatus())) {
            order.setStatus(OrderStatus.SHIPPED);
            addOrderHistory(order, OrderStatus.SHIPPED, "Shipped via " + request.courier().trim() + " (" + awb + ")", updatedBy);
        }
        if (shipped.advanced()) publishTrackingNotification(shipment, ShipmentStatus.SHIPPED);
    }

    @Transactional
    public void handleWebhook(String courierName, byte[] rawBody, String signature) {
        verifySignature(rawBody, signature);
        TrackingWebhookRequest request;
        try {
            request = mapper.readValue(rawBody, TrackingWebhookRequest.class);
        } catch (Exception ex) {
            throw ApiException.badRequest("Malformed courier webhook payload");
        }
        if (request.eventId() == null || request.eventId().isBlank()
                || request.awb() == null || request.awb().isBlank()
                || request.status() == null || request.status().isBlank())
            throw ApiException.badRequest("Courier webhook requires eventId, awb, and status");
        if (request.eventId().length() > 190 || request.awb().length() > 100
                || request.status().length() > 50
                || (request.description() != null && request.description().length() > 500)
                || courierName.length() > 100)
            throw ApiException.badRequest("Courier webhook fields exceed their maximum lengths");
        String courier = normalizeCourier(courierName);
        String awb = request.awb().trim();
        Shipment shipment = shipments.findByCourierNameAndAwbForUpdate(courier, awb)
                .orElseThrow(() -> ApiException.notFound("Shipment not found"));
        if (shipment.getDirection() == ShipmentDirection.REVERSE
                && "PICKUP_FAILED".equals(request.status().trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'))) {
            com.capstore.entity.ReturnRequest returnRequest = shipment.getReturnRequest();
            if (trackingEvents.insertIfAbsent(shipment.getId(), shipment.getCourierName(),
                    request.eventId().trim(), ShipmentStatus.CREATED.name(), request.description(),
                    request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt()) == 0) return;
            returnRequest.setPickupFailures(returnRequest.getPickupFailures() + 1);
            if (returnRequest.getPickupFailures() >= 3 && returnRequest.getStatus() == ReturnStatus.PICKUP_SCHEDULED)
                ReturnTransitions.move(returnRequest, ReturnStatus.CLOSED);
            return;
        }
        ShipmentStatus status = courierClient.mapStatus(request.status());
        EventResult result = recordEvent(shipment, request.eventId().trim(), status,
                request.description(), request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        if (!result.inserted()) return;
        if (request.expectedDelivery() != null) shipment.setExpectedDelivery(request.expectedDelivery());
        if (shipment.getDirection() == ShipmentDirection.REVERSE) {
            com.capstore.entity.ReturnRequest returnRequest = shipment.getReturnRequest();
            ReturnStatus returnStatus = status == ShipmentStatus.DELIVERED ? ReturnStatus.RECEIVED
                    : status == ShipmentStatus.SHIPPED ? ReturnStatus.PICKED_UP : null;
            if (result.advanced() && returnStatus != null && returnRequest.getStatus() != returnStatus)
                ReturnTransitions.move(returnRequest, returnStatus);
            return;
        }
        if (shipment.getReturnRequest() != null) return;
        if (result.advanced()) {
            boolean orderAdvanced = applyOrderStatus(shipment, status);
            CustomerOrder order = shipment.getOrder();
            if (status == ShipmentStatus.DELIVERED && order.getOrderType() == OrderType.REPLACEMENT) {
                returnRequests.findByReplacementOrderId(order.getId()).ifPresent(returnRequest -> {
                    if (returnRequest.getStatus() == ReturnStatus.REPLACEMENT_CREATED)
                        ReturnTransitions.move(returnRequest, ReturnStatus.CLOSED);
                });
            }
            if (orderAdvanced && (status == ShipmentStatus.OUT_FOR_DELIVERY || status == ShipmentStatus.DELIVERED))
                publishTrackingNotification(shipment, status);
        }
    }

    @Transactional
    public void updateFromAdmin(CustomerOrder order, OrderStatus orderStatus, String updatedBy) {
        Shipment shipment = shipments.findFirstByOrderIdAndDirectionAndReturnRequestIsNull(order.getId(), ShipmentDirection.FORWARD)
                .orElseThrow(() -> ApiException.badRequest("Create a shipment before updating its delivery status"));
        ShipmentStatus status = switch (orderStatus) {
            case SHIPPED -> ShipmentStatus.SHIPPED;
            case OUT_FOR_DELIVERY -> ShipmentStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> ShipmentStatus.DELIVERED;
            default -> throw ApiException.badRequest("Unsupported shipment status");
        };
        EventResult result = recordEvent(shipment, "admin-" + status + "-" + UUID.randomUUID(),
                status, "Status updated by " + updatedBy, LocalDateTime.now());
        if (result.advanced()) {
            boolean orderAdvanced = applyOrderStatus(shipment, status);
            if (orderAdvanced && (status == ShipmentStatus.OUT_FOR_DELIVERY || status == ShipmentStatus.DELIVERED))
                publishTrackingNotification(shipment, status);
        }
    }

    @Transactional
    public void createReverseShipment(com.capstore.entity.ReturnRequest request) {
        CustomerOrder order = request.getOrder();
        CourierClient.ReverseShipment label = courierClient.createReverseShipment(order, request);
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setReturnRequest(request);
        shipment.setDirection(ShipmentDirection.REVERSE);
        shipment.setCourierName(normalizeCourier(label.courierName()));
        shipment.setAwb(label.awb());
        shipment.setTrackingUrl(label.trackingUrl());
        shipment.setStatus(ShipmentStatus.CREATED);
        shipments.save(shipment);
        recordEvent(shipment, "reverse-created-" + request.getPublicId(), ShipmentStatus.CREATED,
                "Reverse pickup scheduled", LocalDateTime.now());
    }

    @Transactional
    public void returnItemToCustomer(com.capstore.entity.ReturnRequest request) {
        CourierClient.ReverseShipment label = courierClient.createReturnToCustomer(request.getOrder(), request);
        Shipment shipment = new Shipment();
        shipment.setOrder(request.getOrder());
        shipment.setReturnRequest(request);
        shipment.setDirection(ShipmentDirection.FORWARD);
        shipment.setCourierName(normalizeCourier(label.courierName()));
        shipment.setAwb(label.awb());
        shipment.setTrackingUrl(label.trackingUrl());
        shipment.setStatus(ShipmentStatus.CREATED);
        shipments.save(shipment);
        recordEvent(shipment, "return-to-customer-" + request.getPublicId(), ShipmentStatus.CREATED,
                "QC failed; item returning to customer", LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public TrackingResponse tracking(CustomerOrder order) {
        return shipments.findFirstByOrderIdAndDirectionAndReturnRequestIsNull(order.getId(), ShipmentDirection.FORWARD)
                .map(shipment -> new TrackingResponse(order.getId(), shipment.getStatus(),
                        shipment.getExpectedDelivery(),
                        trackingEvents.findByShipmentIdOrderByOccurredAtDescIdDesc(shipment.getId()).stream()
                                .map(event -> new TrackingEventResponse(event.getCourierName(), shipment.getAwb(),
                                        event.getStatus(), event.getDescription(), event.getOccurredAt()))
                                .toList()))
                .orElseGet(() -> new TrackingResponse(order.getId(), ShipmentStatus.CREATED, null, List.of()));
    }

    private EventResult recordEvent(Shipment shipment, String eventId, ShipmentStatus status,
                                    String description, LocalDateTime occurredAt) {
        int inserted = trackingEvents.insertIfAbsent(shipment.getId(), shipment.getCourierName(),
                eventId, status.name(), description, occurredAt);
        if (inserted == 0) return new EventResult(false, false);
        boolean moved = status.ordinal() > shipment.getStatus().ordinal();
        if (moved) shipment.setStatus(status);
        return new EventResult(true, moved);
    }

    private boolean applyOrderStatus(Shipment shipment, ShipmentStatus status) {
        CustomerOrder order = shipment.getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RETURN_REQUESTED
                || order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.REFUNDED)
            return false;
        OrderStatus next = switch (status) {
            case SHIPPED -> OrderStatus.SHIPPED;
            case OUT_FOR_DELIVERY -> OrderStatus.OUT_FOR_DELIVERY;
            case DELIVERED -> OrderStatus.DELIVERED;
            default -> null;
        };
        if (next != null && orderRank(next) > orderRank(order.getStatus())) {
            order.setStatus(next);
            addOrderHistory(order, next, "Courier update: " + next.name().replace('_', ' ').toLowerCase(Locale.ROOT), "courier");
            return true;
        }
        return false;
    }

    private int orderRank(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> 1;
            case PROCESSING -> 2;
            case PACKED -> 3;
            case SHIPPED -> 4;
            case OUT_FOR_DELIVERY -> 5;
            case DELIVERED -> 6;
            default -> 0;
        };
    }

    private void addOrderHistory(CustomerOrder order, OrderStatus status, String note, String by) {
        OrderStatusHistory entry = new OrderStatusHistory();
        entry.setOrderId(order.getId());
        entry.setStatus(status);
        entry.setNote(note);
        entry.setUpdatedBy(by);
        orderHistory.save(entry);
    }

    private void publishTrackingNotification(Shipment shipment, ShipmentStatus status) {
        CustomerOrder order = shipment.getOrder();
        String type = order.getOrderType() == OrderType.REPLACEMENT && status == ShipmentStatus.SHIPPED
                ? "REPLACEMENT_SHIPPED" : status.name();
        events.publishEvent(new OrderNotificationEvent(order.getId(), type,
                order.getUser().getEmail(), order.getUser().getName(), order.getOrderNo(),
                shipment.getAwb(), shipment.getCourierName(), trackingLink(shipment),
                status.name().replace('_', ' '), shipment.getExpectedDelivery()));
    }

    private String trackingLink(Shipment shipment) {
        if (shipment.getTrackingUrl() != null && !shipment.getTrackingUrl().isBlank())
            return shipment.getTrackingUrl();
        return frontendUrl + "/orders/" + shipment.getOrder().getPublicId();
    }

    private void verifySignature(byte[] body, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank())
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Courier webhook is not configured");
        if (signature == null || signature.isBlank())
            throw ApiException.badRequest("Missing courier webhook signature");
        String provided = signature.startsWith("sha256=") ? signature.substring(7) : signature;
        String expected = hmacHex(body, webhookSecret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                provided.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))
            throw ApiException.badRequest("Invalid courier webhook signature");
    }

    private String hmacHex(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not compute courier webhook signature", ex);
        }
    }

    private String normalizeCourier(String courier) {
        return courier.trim().toUpperCase(Locale.ROOT);
    }

    private record EventResult(boolean inserted, boolean advanced) {}

    private static final EnumSet<OrderStatus> SHIPPABLE = EnumSet.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKED);

}
