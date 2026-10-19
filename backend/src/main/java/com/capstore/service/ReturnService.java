package com.capstore.service;

import com.capstore.dto.ReturnDtos.*;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.*;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {
    private static final EnumSet<ReturnStatus> ACTIVE = EnumSet.of(ReturnStatus.REQUESTED,
            ReturnStatus.APPROVED, ReturnStatus.PICKUP_SCHEDULED, ReturnStatus.PICKED_UP,
            ReturnStatus.RECEIVED, ReturnStatus.QC_PASSED, ReturnStatus.REFUND_INITIATED);
    private final ReturnRequestRepository returns;
    private final RefundRepository refunds;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository history;
    private final ShipmentRepository shipments;
    private final PaymentRepository payments;
    private final ProductVariantRepository variants;
    private final OrderService orderService;
    private final TrackingService tracking;
    private final PaymentGateway gateway;
    private final AuthFacade auth;
    private final ApplicationEventPublisher events;

    @Value("${app.returns.window-days:30}") private int returnWindowDays;
    @Value("${app.frontend-url}") private String frontendUrl;

    @Transactional
    public ReturnResponse create(CreateReturnRequest request) {
        User user = auth.currentUser();
        CustomerOrder order = orders.findByPublicIdAndUserIdForUpdate(request.orderId(), user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED)
            throw ApiException.badRequest("Only delivered orders can be returned");
        OrderStatusHistory delivered = history.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                order.getId(), OrderStatus.DELIVERED).orElseThrow(() -> ApiException.badRequest("Delivery date is unavailable"));
        if (delivered.getCreatedAt().plusDays(returnWindowDays).isBefore(LocalDateTime.now()))
            throw ApiException.badRequest("The return window has closed");
        OrderItem item = orderItems.findByIdAndOrderId(request.orderItemId(), order.getId())
                .orElseThrow(() -> ApiException.notFound("Order item not found"));
        if (!item.getVariant().getProduct().isReturnable())
            throw ApiException.badRequest("This product is not eligible for return");
        if (returns.existsByOrderItemIdAndStatusIn(item.getId(), ACTIVE))
            throw ApiException.conflict("A return request is already open for this item");
        int alreadyReturned = returns.sumCompletedQuantity(item.getId());
        if (request.quantity() > item.getQuantity() - alreadyReturned)
            throw ApiException.badRequest("Return quantity exceeds the remaining purchased quantity");

        ReturnRequest returned = new ReturnRequest();
        returned.setOrder(order);
        returned.setOrderItem(item);
        returned.setType(request.type());
        returned.setReason(request.reason().trim());
        returned.setQuantity(request.quantity());
        returned.setUpiId(request.upiId() == null ? null : request.upiId().trim());
        returned.setRefundAmount(calculateRefund(order, item, request.quantity()));
        return toResponse(returns.saveAndFlush(returned));
    }

    @Transactional(readOnly = true)
    public ReturnResponse getMine(String publicId) {
        User user = auth.currentUser();
        return returns.findByPublicIdAndOrderUserId(publicId, user.getId())
                .map(this::toResponse).orElseThrow(() -> ApiException.notFound("Return request not found"));
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> forOrderMine(String orderPublicId) {
        User user = auth.currentUser();
        CustomerOrder order = orders.findByPublicIdAndUserId(orderPublicId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        return returns.findByOrderIdAndOrderUserIdOrderByCreatedAtDesc(order.getId(), user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> allForAdmin() {
        return returns.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReturnResponse approve(String id, String note) {
        ReturnRequest request = locked(id);
        ReturnTransitions.move(request, ReturnStatus.APPROVED);
        request.setAdminNote(note);
        tracking.createReverseShipment(request);
        ReturnTransitions.move(request, ReturnStatus.PICKUP_SCHEDULED);
        publish(request, "RETURN_APPROVED", null, null);
        publish(request, "PICKUP_SCHEDULED", shipment(request).getAwb(), shipment(request).getCourierName());
        return toResponse(request);
    }

    @Transactional
    public ReturnResponse reject(String id, String note) {
        ReturnRequest request = locked(id);
        ReturnTransitions.move(request, ReturnStatus.REJECTED);
        request.setAdminNote(note);
        return toResponse(request);
    }

    @Transactional
    public ReturnResponse qualityCheck(String id, boolean passed, String note) {
        ReturnRequest request = locked(id);
        request.setQcNote(note);
        ReturnTransitions.move(request, passed ? ReturnStatus.QC_PASSED : ReturnStatus.QC_FAILED);
        if (!passed) {
            ReturnTransitions.move(request, ReturnStatus.CLOSED);
            tracking.returnItemToCustomer(request);
            publish(request, "RETURN_QC_FAILED", null, null);
            return toResponse(request);
        }
        if (request.getType() == ReturnType.REPLACEMENT) {
            try {
                CustomerOrder replacement = orderService.createReplacementOrder(request.getOrder(),
                        request.getOrderItem(), request.getQuantity(), request.getPublicId());
                request.setReplacementOrder(replacement);
                request.setCompletedReturn(true);
                ReturnTransitions.move(request, ReturnStatus.REPLACEMENT_CREATED);
                return toResponse(request);
            } catch (ApiException ex) {
                if (!"Replacement stock unavailable".equals(ex.getMessage())) throw ex;
                request.setAdminNote("Replacement unavailable; refund fallback started");
            }
        }
        initiateRefund(request);
        return toResponse(request);
    }

    @Transactional
    public ReturnResponse confirmCodRefund(String id, String note) {
        ReturnRequest request = locked(id);
        Refund refund = refunds.findByReturnRequestId(request.getId())
                .orElseThrow(() -> ApiException.notFound("Refund not found"));
        if (refund.getPayment() != null) throw ApiException.badRequest("Gateway refunds are confirmed by webhook");
        markRefunded(refund);
        request.setAdminNote(note);
        return toResponse(request);
    }

    @Transactional
    public void confirmRefund(String gatewayRefundId, String gatewayPaymentId, long amountPaise) {
        Refund refund = refunds.findByGatewayRefundId(gatewayRefundId)
                .orElseThrow(() -> ApiException.notFound("Refund not found"));
        if (refund.getStatus() == RefundStatus.REFUNDED) return;
        if (refund.getPayment() == null || !refund.getPayment().getGatewayPaymentId().equals(gatewayPaymentId)
                || refund.getAmount().movePointRight(2).longValueExact() != amountPaise)
            throw ApiException.badRequest("Refund webhook does not match the recorded refund");
        markRefunded(refund);
    }

    @Transactional
    public void failRefund(String gatewayRefundId) {
        refunds.findByGatewayRefundId(gatewayRefundId).ifPresent(refund -> {
            if (refund.getStatus() != RefundStatus.REFUNDED) refund.setStatus(RefundStatus.FAILED);
        });
    }

    @Transactional
    public int retryFailedRefunds() {
        int count = 0;
        for (Refund refund : refunds.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(RefundStatus.FAILED, 3)) {
            invokeGateway(refund);
            count++;
        }
        for (ReturnRequest request : returns.findByStatusAndPickupFailuresGreaterThanEqual(
                ReturnStatus.PICKUP_SCHEDULED, 3)) {
            ReturnTransitions.move(request, ReturnStatus.CLOSED);
        }
        return count;
    }

    private void initiateRefund(ReturnRequest request) {
        BigDecimal paid = payments.sumPaidAmountByOrderId(request.getOrder().getId());
        BigDecimal refunded = refunds.sumRefundedByOrderId(request.getOrder().getId());
        BigDecimal amount = request.getRefundAmount().min(paid.subtract(refunded).max(BigDecimal.ZERO))
                .setScale(2, RoundingMode.HALF_UP);
        Refund refund = refunds.findByReturnRequestId(request.getId()).orElseGet(Refund::new);
        refund.setReturnRequest(request);
        refund.setAmount(amount);
        refund.setIdempotencyKey("return-" + request.getPublicId());
        Payment payment = payments.findByOrderIdAndStatus(request.getOrder().getId(), PaymentStatus.PAID)
                .stream().findFirst().orElse(null);
        boolean cod = payment != null && "COD".equalsIgnoreCase(payment.getMethod());
        if (!cod && amount.signum() > 0 && (payment == null || payment.getGatewayPaymentId() == null))
            throw ApiException.badRequest("No captured payment is available for refund");
        if (cod && (request.getUpiId() == null || request.getUpiId().isBlank()))
            throw ApiException.badRequest("Add your UPI ID to receive the COD refund");
        refund.setPayment(cod ? null : payment);
        refund.setStatus(amount.signum() == 0 ? RefundStatus.REFUNDED : RefundStatus.INITIATED);
        if (cod) refund.setStatus(RefundStatus.INITIATED);
        refunds.saveAndFlush(refund);
        ReturnTransitions.move(request, ReturnStatus.REFUND_INITIATED);
        if (amount.signum() == 0) markRefunded(refund);
        else if (!cod) invokeGateway(refund);
    }

    private void invokeGateway(Refund refund) {
        if (refund.getAttempts() >= 3 || refund.getPayment() == null) return;
        refund.setAttempts(refund.getAttempts() + 1);
        refund.setStatus(RefundStatus.INITIATED);
        try {
            PaymentGateway.RefundDetails result = gateway.refund(refund.getPayment().getGatewayPaymentId(),
                    refund.getAmount().movePointRight(2).longValueExact(), refund.getIdempotencyKey());
            if (result.refundId() != null) refund.setGatewayRefundId(result.refundId());
        } catch (RuntimeException ex) {
            refund.setStatus(RefundStatus.FAILED);
            log.error("Refund attempt {} failed for return {}", refund.getAttempts(),
                    refund.getReturnRequest().getPublicId(), ex);
        }
    }

    private void markRefunded(Refund refund) {
        refund.setStatus(RefundStatus.REFUNDED);
        refunds.flush();
        ReturnRequest request = refund.getReturnRequest();
        if (request.getStatus() == ReturnStatus.REFUND_INITIATED) {
            ReturnTransitions.move(request, ReturnStatus.REFUNDED);
            request.setCompletedReturn(true);
            BigDecimal paid = payments.sumPaidAmountByOrderId(request.getOrder().getId());
            BigDecimal totalRefunded = refunds.sumRefundedByOrderId(request.getOrder().getId());
            if (totalRefunded.compareTo(paid) >= 0) request.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
            publish(request, "REFUND_PROCESSED", null, null);
        }
    }

    private BigDecimal calculateRefund(CustomerOrder order, OrderItem item, int quantity) {
        BigDecimal returnedValue = item.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        if (order.getSubtotal() == null || order.getSubtotal().signum() == 0 || order.getDiscount() == null)
            return returnedValue.setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocatedDiscount = order.getDiscount().multiply(returnedValue)
                .divide(order.getSubtotal(), 2, RoundingMode.HALF_UP);
        return returnedValue.subtract(allocatedDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private ReturnRequest locked(String publicId) {
        return returns.findByPublicIdForUpdate(publicId).orElseThrow(() -> ApiException.notFound("Return request not found"));
    }

    private Shipment shipment(ReturnRequest request) {
        return shipments.findFirstByReturnRequestIdAndDirectionOrderByIdDesc(request.getId(), ShipmentDirection.REVERSE)
                .orElseThrow(() -> ApiException.notFound("Pickup not found"));
    }

    private void publish(ReturnRequest request, String type, String awb, String courier) {
        CustomerOrder order = request.getOrder();
        String link = frontendUrl + "/orders/" + order.getPublicId();
        events.publishEvent(new OrderNotificationEvent(order.getId(), type, order.getUser().getEmail(),
                order.getUser().getName(), order.getOrderNo(), awb == null ? request.getPublicId() : awb,
                courier, link, request.getStatus().name().replace('_', ' '), null));
    }

    private ReturnResponse toResponse(ReturnRequest request) {
        Shipment pickup = shipments.findFirstByReturnRequestIdAndDirectionOrderByIdDesc(
                request.getId(), ShipmentDirection.REVERSE).orElse(null);
        boolean manualRefund = refunds.findByReturnRequestId(request.getId())
                .map(refund -> refund.getPayment() == null).orElse(false);
        return new ReturnResponse(request.getPublicId(), request.getOrder().getPublicId(),
                request.getReplacementOrder() == null ? null : request.getReplacementOrder().getPublicId(),
                request.getOrderItem().getId(), request.getOrderItem().getProductName(), request.getType(),
                request.getStatus(), request.getReason(), request.getAdminNote(), request.getQcNote(),
                request.getQuantity(), request.getRefundAmount(), request.getUpiId(),
                pickup == null ? null : pickup.getCourierName(), pickup == null ? null : pickup.getAwb(),
                pickup == null ? null : pickup.getTrackingUrl(), request.getPickupFailures(), request.getCreatedAt(),
                manualRefund);
    }
}
