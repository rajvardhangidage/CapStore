package com.capstore.service;

import com.capstore.dto.OrderDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.*;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Set<OrderStatus> USER_CANCELLABLE =
            EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

    private final OrderRepository orders;
    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final OrderStatusHistoryRepository history;
    private final PricingService pricing;
    private final InventoryService inventory;
    private final CouponService coupons;
    private final NotificationService notifier;
    private final TrackingService tracking;
    private final AuthFacade auth;
    private final StoreSettingsService storeSettings;

    @Value("${app.order.payment-timeout-minutes}") private long paymentTimeoutMinutes;

    // ---------- checkout ----------
    @Transactional
    public OrderResponse place(PlaceOrderRequest r) {
        User user = auth.currentUser();
        var previous = orders.findByUserIdAndCheckoutRequestKey(user.getId(), r.checkoutRequestKey());
        if (previous.isPresent()) {
            CustomerOrder existing = previous.get();
            if (existing.getStatus() != OrderStatus.CANCELLED) return toResponse(existing);
            existing.setCheckoutRequestKey(null);
            orders.flush();
        }
        Cart cart = carts.findByUserId(user.getId()).orElseThrow(() -> ApiException.badRequest("Your cart is empty"));
        if (cart.getItems().isEmpty()) throw ApiException.badRequest("Your cart is empty");

        CustomerOrder o = new CustomerOrder();
        o.setOrderNo("CAP" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + ThreadLocalRandom.current().nextInt(10000, 100000));
        o.setUser(user);
        o.setCheckoutRequestKey(r.checkoutRequestKey());
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setShipName(r.shipName().trim());
        o.setShipPhone(r.shipPhone());
        o.setShipLine1(r.shipLine1().trim());
        o.setShipLine2(r.shipLine2());
        o.setShipCity(r.shipCity().trim());
        o.setShipState(r.shipState().trim());
        o.setShipPincode(r.shipPincode());

        // Lock variants in a consistent order (by id) to avoid deadlocks
        List<CartItem> lines = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getVariant().getId())).toList();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : lines) {
            ProductVariant v = ci.getVariant();
            BigDecimal unit = pricing.unitPrice(v, user, ci.getQuantity());
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(ci.getQuantity()));
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setVariant(v);
            oi.setProductName(v.getProduct().getName());
            oi.setSku(v.getSku());
            oi.setSize(v.getSize());
            oi.setColor(v.getColor());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(unit);
            oi.setLineTotal(lineTotal);
            o.getItems().add(oi);
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        if (r.couponCode() != null && !r.couponCode().isBlank()) {
            Coupon coupon = coupons.validate(r.couponCode(), subtotal);
            discount = CouponService.discountFor(coupon, subtotal);
            if (coupons.consume(coupon.getId()) == 0) throw ApiException.badRequest("This coupon has reached its usage limit");
            o.setCouponCode(coupon.getCode());
        }
        BigDecimal taxable = subtotal.subtract(discount);
        var settings = storeSettings.get();
        BigDecimal tax = taxable.multiply(settings.gstRatePercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal shipping = taxable.compareTo(settings.freeShippingAbove()) >= 0
                ? BigDecimal.ZERO : settings.shippingFlatRate();

        o.setSubtotal(subtotal);
        o.setDiscount(discount);
        o.setTax(tax);
        o.setGstRatePercent(settings.gstRatePercent());
        o.setShipping(shipping.setScale(2, RoundingMode.HALF_UP));
        o.setTotal(taxable.add(tax).add(o.getShipping()));
        orders.saveAndFlush(o);

        // Reserve stock now (released again on cancel / payment timeout)
        for (CartItem ci : lines) {
            ProductVariant locked = variants.findByIdForUpdate(ci.getVariant().getId())
                    .orElseThrow(() -> ApiException.notFound("Variant not found"));
            inventory.adjust(locked, -ci.getQuantity(), "ORDER_PLACED", o.getId());
        }

        addHistory(o.getId(), OrderStatus.PENDING_PAYMENT, "Order placed, awaiting payment", user.getEmail());
        return toResponse(o);
    }

    /** Removes purchased quantities from the cart only after payment succeeds. */
    @Transactional
    public void removePurchasedItemsFromCart(CustomerOrder order) {
        carts.findByUserId(order.getUser().getId()).ifPresent(cart -> {
            for (OrderItem purchased : order.getItems()) {
                cart.getItems().stream()
                        .filter(item -> item.getVariant().getId().equals(purchased.getVariant().getId()))
                        .findFirst()
                        .ifPresent(item -> {
                            int remaining = item.getQuantity() - purchased.getQuantity();
                            if (remaining > 0) item.setQuantity(remaining);
                            else cart.getItems().remove(item);
                        });
            }
        });
    }

    @Transactional
    public CustomerOrder createReplacementOrder(CustomerOrder original, OrderItem returnedItem,
                                                int quantity, String returnPublicId) {
        ProductVariant variant = variants.findByIdForUpdate(returnedItem.getVariant().getId())
                .orElseThrow(() -> ApiException.notFound("Product variant not found"));
        if (variant.getStockQty() < quantity) throw ApiException.badRequest("Replacement stock unavailable");

        CustomerOrder replacement = new CustomerOrder();
        replacement.setOrderNo("RPL" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + ThreadLocalRandom.current().nextInt(10000, 100000));
        replacement.setUser(original.getUser());
        replacement.setOriginalOrder(original);
        replacement.setOrderType(OrderType.REPLACEMENT);
        replacement.setStatus(OrderStatus.CONFIRMED);
        replacement.setPaymentStatus(PaymentStatus.PAID);
        replacement.setShipName(original.getShipName());
        replacement.setShipPhone(original.getShipPhone());
        replacement.setShipLine1(original.getShipLine1());
        replacement.setShipLine2(original.getShipLine2());
        replacement.setShipCity(original.getShipCity());
        replacement.setShipState(original.getShipState());
        replacement.setShipPincode(original.getShipPincode());
        replacement.setSubtotal(BigDecimal.ZERO);
        replacement.setDiscount(BigDecimal.ZERO);
        replacement.setTax(BigDecimal.ZERO);
        replacement.setGstRatePercent(original.getGstRatePercent());
        replacement.setShipping(BigDecimal.ZERO);
        replacement.setTotal(BigDecimal.ZERO);
        OrderItem replacementItem = new OrderItem();
        replacementItem.setOrder(replacement);
        replacementItem.setVariant(variant);
        replacementItem.setProductName(returnedItem.getProductName());
        replacementItem.setSku(returnedItem.getSku());
        replacementItem.setSize(returnedItem.getSize());
        replacementItem.setColor(returnedItem.getColor());
        replacementItem.setQuantity(quantity);
        replacementItem.setUnitPrice(BigDecimal.ZERO);
        replacementItem.setLineTotal(BigDecimal.ZERO);
        replacement.getItems().add(replacementItem);
        orders.saveAndFlush(replacement);
        inventory.adjust(variant, -quantity, "REPLACEMENT_ORDER", replacement.getId());
        addHistory(replacement.getId(), OrderStatus.CONFIRMED, "Replacement for return " + returnPublicId, "system");
        tracking.paymentConfirmed(replacement);
        return replacement;
    }

    // ---------- customer views ----------
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(int page, int size) {
        User u = auth.currentUser();
        var p = orders.findByUserIdOrderByCreatedAtDesc(u.getId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PageResponse.of(p, this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(String publicId) { return toResponse(findMine(publicId)); }

    @Transactional(readOnly = true)
    public List<TimelineEntry> timelineMine(String publicId) {
        CustomerOrder o = findMine(publicId);
        return timeline(o.getId());
    }

    @Transactional(readOnly = true)
    public TrackingResponse trackingMine(String publicId) {
        return tracking.tracking(findMine(publicId));
    }

    @Transactional
    public OrderResponse cancelMine(String publicId) {
        CustomerOrder o = findMine(publicId);
        if (!USER_CANCELLABLE.contains(o.getStatus()))
            throw ApiException.badRequest("This order can no longer be cancelled");
        cancelInternal(o, "Cancelled by customer", o.getUser().getEmail());
        notifier.statusChanged(o, o.getPaymentStatus() == PaymentStatus.REFUND_PENDING ? "Your refund will be processed shortly." : null);
        return toResponse(o);
    }

    // ---------- shared logic ----------
    public List<TimelineEntry> timeline(Long orderId) {
        return history.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(h -> new TimelineEntry(h.getStatus(), h.getNote(), h.getCreatedAt())).toList();
    }

    /** Cancels an order, returns reserved stock and coupon use, and flags a refund if already paid. */
    public void cancelInternal(CustomerOrder o, String note, String by) {
        List<OrderItem> items = o.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getVariant().getId())).toList();
        for (OrderItem i : items) {
            ProductVariant v = variants.findByIdForUpdate(i.getVariant().getId())
                    .orElseThrow(() -> ApiException.notFound("Variant not found"));
            inventory.adjust(v, i.getQuantity(), "ORDER_CANCELLED", o.getId());
        }
        if (o.getCouponCode() != null) coupons.release(o.getCouponCode());
        o.setPaymentStatus(o.getPaymentStatus() == PaymentStatus.PAID ? PaymentStatus.REFUND_PENDING : PaymentStatus.FAILED);
        o.setStatus(OrderStatus.CANCELLED);
        addHistory(o.getId(), OrderStatus.CANCELLED, note, by);
    }

    /** Called by the scheduler: cancel unpaid orders past the payment timeout. */
    @Transactional
    public int cancelExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        List<CustomerOrder> stale = orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        for (CustomerOrder o : stale) {
            cancelInternal(o, "Auto-cancelled: payment not completed in time", "system");
        }
        return stale.size();
    }

    public void addHistory(Long orderId, OrderStatus status, String note, String by) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setStatus(status);
        h.setNote(note);
        h.setUpdatedBy(by);
        history.save(h);
    }

    public CustomerOrder findMine(String publicId) {
        User u = auth.currentUser();
        return orders.findByPublicIdAndUserId(publicId, u.getId()).orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    public OrderResponse toResponse(CustomerOrder o) {
        List<OrderItemResponse> items = o.getItems().stream().map(i -> new OrderItemResponse(
                i.getId(), i.getProductName(), i.getSku(), i.getSize(), i.getColor(),
                i.getQuantity(), i.getUnitPrice(), i.getLineTotal(),
                i.getVariant().getProduct().isReturnable())).toList();
        ShippingAddress addr = new ShippingAddress(o.getShipName(), o.getShipPhone(), o.getShipLine1(),
                o.getShipLine2(), o.getShipCity(), o.getShipState(), o.getShipPincode());
        return new OrderResponse(o.getId(), o.getPublicId(), o.getOrderNo(), o.getStatus(), o.getPaymentStatus(),
                o.getSubtotal(), o.getDiscount(), o.getTax(), o.getShipping(), o.getTotal(), addr,
                o.getCourier(), o.getTrackingNo(), o.getTrackingUrl(),
                o.getUser().getName(), o.getUser().getEmail(), items, o.getCreatedAt(), o.getCouponCode());
    }
}
