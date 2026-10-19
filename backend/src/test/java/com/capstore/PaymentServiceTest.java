package com.capstore;

import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderRepository;
import com.capstore.repository.*;
import com.capstore.security.AuthFacade;
import com.capstore.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.ApplicationEventPublisher;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private PaymentRepository payments;
    private OrderRepository orders;
    private OrderStatusHistoryRepository history;
    private ApplicationEventPublisher events;
    private UserRepository users;
    private AuthFacade auth;
    private PaymentGateway gateway;
    private TrackingService tracking;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        orders = mock(OrderRepository.class);
        history = mock(OrderStatusHistoryRepository.class);
        events = mock(ApplicationEventPublisher.class);
        users = mock(UserRepository.class);
        auth = new AuthFacade(users);
        gateway = mock(PaymentGateway.class);
        ReturnRequestRepository returnRepo = mock(ReturnRequestRepository.class);
        RefundRepository refundRepo = mock(RefundRepository.class);
        PaymentRepository paymentRepo = payments;
        tracking = new TrackingService(mock(com.capstore.repository.ShipmentRepository.class),
                mock(com.capstore.repository.TrackingEventRepository.class), returnRepo, history, new MockCourierClient(),
                new ObjectMapper(), events);
        OrderService orderService = new OrderService(orders, mock(CartRepository.class),
                mock(ProductVariantRepository.class), history, new PricingService(),
                new InventoryService(mock(InventoryLogRepository.class)), null, null, tracking, auth, null);
        ReturnService returnService = new ReturnService(returnRepo, refundRepo, orders, mock(OrderItemRepository.class),
                history, mock(ShipmentRepository.class), paymentRepo, mock(ProductVariantRepository.class),
                orderService, tracking, gateway, auth, events);
        ReflectionTestUtils.setField(returnService, "frontendUrl", "http://localhost");
        service = new PaymentService(payments, orders, orderService, tracking, auth, new ObjectMapper(), gateway, returnService);
        ReflectionTestUtils.setField(service, "devMode", false);
        ReflectionTestUtils.setField(service, "keySecret", "key-secret");
        ReflectionTestUtils.setField(service, "webhookSecret", "webhook-secret");
    }

    @Test
    void rejectsCapturedWebhookWithMismatchedAmount() throws Exception {
        Payment payment = payment();
        when(payments.findByGatewayOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1","amount":100,"currency":"INR","status":"captured"}}}}
                """;

        assertThrows(ApiException.class, () -> service.handleWebhook(body, signature(body)));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verifyNoInteractions(events);
    }

    @Test
    void ignoresFailedWebhookAfterPaymentHasAlreadyBeenMarkedPaid() throws Exception {
        Payment payment = payment();
        payment.setStatus(PaymentStatus.PAID);
        when(payments.findByGatewayOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        String body = """
                {"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1"}}}}
                """;

        service.handleWebhook(body, signature(body));

        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    void flagsSecondCapturedPaymentForRefundInsteadOfSilentlyAcceptingIt() throws Exception {
        Payment payment = payment();
        payment.getOrder().setStatus(OrderStatus.CONFIRMED);
        payment.getOrder().setPaymentStatus(PaymentStatus.PAID);
        when(payments.findByGatewayOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_duplicate","order_id":"order_1","amount":1250,"currency":"INR","status":"captured"}}}}
                """;

        service.handleWebhook(body, signature(body));

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, payment.getOrder().getPaymentStatus());
    }

    @Test
    void flagsDistinctPaymentIdOnAlreadyPaidGatewayOrderForRefund() throws Exception {
        Payment payment = payment();
        payment.setStatus(PaymentStatus.PAID);
        payment.setGatewayPaymentId("pay_first");
        payment.getOrder().setStatus(OrderStatus.CONFIRMED);
        payment.getOrder().setPaymentStatus(PaymentStatus.PAID);
        when(payments.findByGatewayOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_second","order_id":"order_1","amount":1250,"currency":"INR","status":"captured"}}}}
                """;

        service.handleWebhook(body, signature(body));

        assertEquals(PaymentStatus.REFUND_PENDING, payment.getOrder().getPaymentStatus());
    }

    @Test
    void marksSuccessfulCapturePaidAndPublishesConfirmation() throws Exception {
        Payment payment = payment();
        when(payments.findByGatewayOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        String body = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1","amount":1250,"currency":"INR","status":"captured","method":"card"}}}}
                """;

        service.handleWebhook(body, signature(body));

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentStatus.PAID, payment.getOrder().getPaymentStatus());
        verify(events).publishEvent(any(OrderNotificationEvent.class));
    }

    @Test
    void doesNotMarkAuthorizedButUncapturedBrowserPaymentPaid() throws Exception {
        Payment payment = payment();
        User user = payment.getOrder().getUser();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getEmail(), "token", org.springframework.security.core.authority.AuthorityUtils.NO_AUTHORITIES));
        when(payments.findByGatewayOrderIdForUpdate("order_1")).thenReturn(Optional.of(payment));
        when(gateway.fetchPayment("pay_1")).thenReturn(
                new PaymentGateway.PaymentDetails("order_1", 1250, "INR", "authorized", "card"));
        String signature = paymentSignature("order_1|pay_1");

        assertThrows(ApiException.class, () -> service.verify(
                new com.capstore.dto.OrderDtos.VerifyPaymentRequest(9L, "order_1", "pay_1", signature)));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    private Payment payment() {
        User user = new User();
        user.setId(5L);
        user.setName("Customer");
        user.setEmail("customer@example.com");
        CustomerOrder order = new CustomerOrder();
        order.setId(9L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setGatewayOrderId("order_1");
        payment.setAmount(new BigDecimal("12.50"));
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private String signature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private String paymentSignature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("key-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }
}
