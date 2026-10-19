package com.capstore;

import com.capstore.dto.ReturnDtos.CreateReturnRequest;
import com.capstore.dto.ReturnDtos.ReturnResponse;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.*;
import com.capstore.security.AuthFacade;
import com.capstore.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReturnServiceTest {
    private ReturnRequestRepository returns;
    private RefundRepository refunds;
    private OrderRepository orders;
    private OrderItemRepository items;
    private OrderStatusHistoryRepository history;
    private ShipmentRepository shipments;
    private PaymentRepository payments;
    private ProductVariantRepository variants;
    private PaymentGateway gateway;
    private ApplicationEventPublisher events;
    private User user;
    private ReturnService service;

    @BeforeEach
    void setUp() {
        returns = mock(ReturnRequestRepository.class);
        refunds = mock(RefundRepository.class);
        orders = mock(OrderRepository.class);
        items = mock(OrderItemRepository.class);
        history = mock(OrderStatusHistoryRepository.class);
        shipments = mock(ShipmentRepository.class);
        payments = mock(PaymentRepository.class);
        variants = mock(ProductVariantRepository.class);
        gateway = mock(PaymentGateway.class);
        events = mock(ApplicationEventPublisher.class);
        UserRepository users = mock(UserRepository.class);
        user = new User();
        user.setId(7L);
        user.setName("Customer");
        user.setEmail("customer@example.com");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), "token", AuthorityUtils.NO_AUTHORITIES));
        AuthFacade auth = new AuthFacade(users);
        TrackingService tracking = new TrackingService(shipments, mock(TrackingEventRepository.class),
                returns, history, new MockCourierClient(), new com.fasterxml.jackson.databind.ObjectMapper(), events);
        OrderService orderService = new OrderService(orders, mock(CartRepository.class), variants, history,
                new PricingService(), new InventoryService(mock(InventoryLogRepository.class)), null, null,
                tracking, auth, null);
        service = new ReturnService(returns, refunds, orders, items, history, shipments, payments, variants,
                orderService, tracking, gateway, auth, events);
        ReflectionTestUtils.setField(service, "returnWindowDays", 30);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://shop.example");
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void rejectsReturnWhenOrderIsNotDeliveredOrProductIsNotReturnable() {
        CustomerOrder order = order();
        order.setStatus(OrderStatus.SHIPPED);
        when(orders.findByPublicIdAndUserIdForUpdate("order-public", 7L)).thenReturn(Optional.of(order));

        assertThrows(ApiException.class, () -> service.create(request(ReturnType.REFUND, 1)));
        verifyNoInteractions(items);
    }

    @Test
    void rejectsReturnAfterWindowExpired() {
        CustomerOrder order = order();
        when(orders.findByPublicIdAndUserIdForUpdate("order-public", 7L)).thenReturn(Optional.of(order));
        OrderStatusHistory delivered = new OrderStatusHistory();
        delivered.setCreatedAt(LocalDateTime.now().minusDays(31));
        when(history.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(44L, OrderStatus.DELIVERED))
                .thenReturn(Optional.of(delivered));

        assertThrows(ApiException.class, () -> service.create(request(ReturnType.REFUND, 1)));
        verifyNoInteractions(items);
    }

    @Test
    void rejectsReturnForNonReturnableProduct() {
        CustomerOrder order = order();
        OrderItem item = item(order);
        item.getVariant().getProduct().setReturnable(false);
        eligibleOrder(order, item);
        when(returns.existsByOrderItemIdAndStatusIn(eq(55L), anyCollection())).thenReturn(false);
        when(returns.sumCompletedQuantity(55L)).thenReturn(0);

        assertThrows(ApiException.class, () -> service.create(request(ReturnType.REFUND, 1)));
        verify(returns, never()).saveAndFlush(any());
    }

    @Test
    void rejectsQuantityAboveUnreturnedPurchasedAmount() {
        CustomerOrder order = order();
        OrderItem item = item(order);
        eligibleOrder(order, item);
        when(returns.existsByOrderItemIdAndStatusIn(eq(55L), anyCollection())).thenReturn(false);
        when(returns.sumCompletedQuantity(55L)).thenReturn(1);

        assertThrows(ApiException.class, () -> service.create(request(ReturnType.REFUND, 2)));
        verify(returns, never()).saveAndFlush(any());
    }

    @Test
    void preventsDuplicateOpenReturnRequestForSameItem() {
        CustomerOrder order = order();
        OrderItem item = item(order);
        eligibleOrder(order, item);
        when(returns.existsByOrderItemIdAndStatusIn(eq(55L), anyCollection())).thenReturn(true);

        assertThrows(ApiException.class, () -> service.create(request(ReturnType.REFUND, 1)));
        verify(returns, never()).saveAndFlush(any());
    }

    @Test
    void calculatesRefundFromStoredPriceAndApportionsOrderDiscount() {
        CustomerOrder order = order();
        OrderItem item = item(order);
        eligibleOrder(order, item);
        when(returns.existsByOrderItemIdAndStatusIn(eq(55L), anyCollection())).thenReturn(false);
        when(returns.sumCompletedQuantity(55L)).thenReturn(0);
        when(returns.saveAndFlush(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnResponse response = service.create(request(ReturnType.REFUND, 1));

        assertEquals(new BigDecimal("40.00"), response.refundAmount());
    }

    @Test
    void capsRefundAmountAtPaidAmountAfterPreviousRefunds() {
        ReturnRequest request = returnRequest(ReturnType.REFUND, ReturnStatus.RECEIVED);
        request.setRefundAmount(new BigDecimal("30.00"));
        when(returns.findByPublicIdForUpdate("return-public")).thenReturn(Optional.of(request));
        when(payments.sumPaidAmountByOrderId(44L)).thenReturn(new BigDecimal("100.00"));
        when(refunds.sumRefundedByOrderId(44L)).thenReturn(new BigDecimal("90.00"));
        Payment payment = new Payment();
        payment.setGatewayPaymentId("pay-captured");
        when(payments.findByOrderIdAndStatus(44L, PaymentStatus.PAID)).thenReturn(java.util.List.of(payment));
        when(refunds.findByReturnRequestId(request.getId())).thenReturn(Optional.empty());
        when(refunds.saveAndFlush(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gateway.refund("pay-captured", 1000L, "return-return-public"))
                .thenReturn(new PaymentGateway.RefundDetails("refund-1", "pending"));
        ReturnResponse response = service.qualityCheck("return-public", true, "Passed");

        assertEquals(ReturnStatus.REFUND_INITIATED, response.status());
        verify(gateway).refund("pay-captured", 1000L, "return-return-public");
        verify(refunds).saveAndFlush(argThat(refund -> refund.getAmount().compareTo(new BigDecimal("10.00")) == 0));
    }

    @Test
    void ignoresDuplicateRefundWebhookAfterFirstConfirmation() {
        ReturnRequest request = returnRequest(ReturnType.REFUND, ReturnStatus.REFUND_INITIATED);
        Refund refund = new Refund();
        refund.setId(2L);
        refund.setReturnRequest(request);
        refund.setAmount(new BigDecimal("10.00"));
        refund.setStatus(RefundStatus.INITIATED);
        Payment payment = new Payment();
        payment.setGatewayPaymentId("pay-captured");
        refund.setPayment(payment);
        when(refunds.findByGatewayRefundId("refund-1")).thenReturn(Optional.of(refund));
        when(payments.sumPaidAmountByOrderId(44L)).thenReturn(new BigDecimal("100.00"));
        when(refunds.sumRefundedByOrderId(44L)).thenReturn(new BigDecimal("10.00"));

        service.confirmRefund("refund-1", "pay-captured", 1000L);
        service.confirmRefund("refund-1", "pay-captured", 1000L);

        assertEquals(RefundStatus.REFUNDED, refund.getStatus());
        assertEquals(ReturnStatus.REFUNDED, request.getStatus());
        verify(events, times(1)).publishEvent(any(OrderNotificationEvent.class));
    }

    @Test
    void rejectsInvalidReturnStatusTransition() {
        ReturnRequest request = returnRequest(ReturnType.REFUND, ReturnStatus.RECEIVED);
        when(returns.findByPublicIdForUpdate("return-public")).thenReturn(Optional.of(request));

        assertThrows(ApiException.class, () -> service.approve("return-public", "approve"));
    }

    @Test
    void fallsBackToRefundWhenReplacementStockIsUnavailable() {
        ReturnRequest request = returnRequest(ReturnType.REPLACEMENT, ReturnStatus.RECEIVED);
        request.setRefundAmount(new BigDecimal("100.00"));
        request.getOrderItem().getVariant().setStockQty(0);
        when(returns.findByPublicIdForUpdate("return-public")).thenReturn(Optional.of(request));
        when(variants.findByIdForUpdate(66L)).thenReturn(Optional.of(request.getOrderItem().getVariant()));
        when(payments.sumPaidAmountByOrderId(44L)).thenReturn(new BigDecimal("200.00"));
        when(refunds.sumRefundedByOrderId(44L)).thenReturn(BigDecimal.ZERO);
        Payment payment = new Payment();
        payment.setGatewayPaymentId("pay-captured");
        when(payments.findByOrderIdAndStatus(44L, PaymentStatus.PAID)).thenReturn(java.util.List.of(payment));
        when(refunds.findByReturnRequestId(request.getId())).thenReturn(Optional.empty());
        when(refunds.saveAndFlush(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gateway.refund("pay-captured", 10000L, "return-return-public"))
                .thenReturn(new PaymentGateway.RefundDetails("refund-2", "pending"));
        ReturnResponse response = service.qualityCheck("return-public", true, "Passed");

        assertEquals(ReturnStatus.REFUND_INITIATED, response.status());
        assertEquals("Replacement unavailable; refund fallback started", request.getAdminNote());
        verify(gateway).refund("pay-captured", 10000L, "return-return-public");
    }

    private void eligibleOrder(CustomerOrder order, OrderItem item) {
        when(orders.findByPublicIdAndUserIdForUpdate("order-public", 7L)).thenReturn(Optional.of(order));
        OrderStatusHistory delivered = new OrderStatusHistory();
        delivered.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(history.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(44L, OrderStatus.DELIVERED))
                .thenReturn(Optional.of(delivered));
        when(items.findByIdAndOrderId(55L, 44L)).thenReturn(Optional.of(item));
    }

    private CustomerOrder order() {
        CustomerOrder order = new CustomerOrder();
        order.setId(44L);
        order.setPublicId("order-public");
        order.setUser(user);
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setSubtotal(new BigDecimal("100.00"));
        order.setDiscount(new BigDecimal("20.00"));
        order.setShipName("Customer");
        order.setShipLine1("Road");
        order.setShipCity("City");
        order.setShipState("State");
        order.setShipPincode("123456");
        return order;
    }

    private OrderItem item(CustomerOrder order) {
        Product product = new Product();
        product.setReturnable(true);
        ProductVariant variant = new ProductVariant();
        variant.setId(66L);
        variant.setProduct(product);
        variant.setStockQty(5);
        OrderItem item = new OrderItem();
        item.setId(55L);
        item.setOrder(order);
        item.setVariant(variant);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        item.setProductName("Cap");
        item.setSku("CAP-1");
        return item;
    }

    private ReturnRequest returnRequest(ReturnType type, ReturnStatus status) {
        CustomerOrder order = order();
        ReturnRequest request = new ReturnRequest();
        request.setId(88L);
        request.setPublicId("return-public");
        request.setOrder(order);
        request.setOrderItem(item(order));
        request.setType(type);
        request.setStatus(status);
        request.setQuantity(1);
        request.setRefundAmount(new BigDecimal("10.00"));
        return request;
    }

    private CreateReturnRequest request(ReturnType type, int quantity) {
        return new CreateReturnRequest("order-public", 55L, type, "Reason", quantity, null);
    }
}
