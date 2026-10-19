package com.capstore;

import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderStatusHistoryRepository;
import com.capstore.repository.ShipmentRepository;
import com.capstore.repository.TrackingEventRepository;
import com.capstore.repository.ReturnRequestRepository;
import com.capstore.service.MockCourierClient;
import com.capstore.service.OrderNotificationEvent;
import com.capstore.service.TrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrackingServiceTest {
    private ShipmentRepository shipments;
    private TrackingEventRepository trackingEvents;
    private OrderStatusHistoryRepository orderHistory;
    private ApplicationEventPublisher events;
    private TrackingService tracking;

    @BeforeEach
    void setUp() {
        shipments = mock(ShipmentRepository.class);
        trackingEvents = mock(TrackingEventRepository.class);
        orderHistory = mock(OrderStatusHistoryRepository.class);
        events = mock(ApplicationEventPublisher.class);
        tracking = new TrackingService(shipments, trackingEvents, mock(ReturnRequestRepository.class), orderHistory, new MockCourierClient(),
                new ObjectMapper(), events);
        ReflectionTestUtils.setField(tracking, "webhookSecret", "test-secret");
    }

    @Test
    void verifiesWebhookAndAdvancesShipmentStatus() throws Exception {
        CustomerOrder order = order();
        Shipment shipment = new Shipment();
        shipment.setId(12L);
        shipment.setOrder(order);
        shipment.setCourierName("ACME");
        shipment.setAwb("AWB-123");
        shipment.setStatus(ShipmentStatus.SHIPPED);
        when(shipments.findByCourierNameAndAwbForUpdate("ACME", "AWB-123")).thenReturn(Optional.of(shipment));
        when(trackingEvents.insertIfAbsent(eq(12L), eq("ACME"), eq("evt-1"), eq("OUT_FOR_DELIVERY"),
                any(), any())).thenReturn(1);

        String body = """
                {"eventId":"evt-1","awb":"AWB-123","status":"OUT_FOR_DELIVERY","description":"At local hub"}
                """;
        tracking.handleWebhook("acme", body.getBytes(StandardCharsets.UTF_8), signature(body));

        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY, shipment.getStatus());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());
        verify(orderHistory).save(any(OrderStatusHistory.class));
        verify(events).publishEvent(any(OrderNotificationEvent.class));
    }

    @Test
    void rejectsInvalidWebhookSignatureBeforeLookup() {
        String body = "{\"eventId\":\"evt-1\",\"awb\":\"AWB-123\",\"status\":\"DELIVERED\"}";

        assertThrows(ApiException.class,
                () -> tracking.handleWebhook("acme", body.getBytes(StandardCharsets.UTF_8), "invalid"));
        verifyNoInteractions(shipments, trackingEvents);
    }

    @Test
    void doesNotNotifyForCourierUpdatesAfterOrderWasCancelled() throws Exception {
        CustomerOrder order = order();
        order.setStatus(OrderStatus.CANCELLED);
        Shipment shipment = new Shipment();
        shipment.setId(12L);
        shipment.setOrder(order);
        shipment.setCourierName("ACME");
        shipment.setAwb("AWB-123");
        shipment.setStatus(ShipmentStatus.SHIPPED);
        when(shipments.findByCourierNameAndAwbForUpdate("ACME", "AWB-123")).thenReturn(Optional.of(shipment));
        when(trackingEvents.insertIfAbsent(eq(12L), eq("ACME"), eq("evt-cancelled"), eq("DELIVERED"),
                any(), any())).thenReturn(1);
        String body = """
                {"eventId":"evt-cancelled","awb":"AWB-123","status":"DELIVERED"}
                """;

        tracking.handleWebhook("acme", body.getBytes(StandardCharsets.UTF_8), signature(body));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verifyNoInteractions(events);
    }

    private CustomerOrder order() {
        User user = new User();
        user.setName("Customer");
        user.setEmail("customer@example.com");
        CustomerOrder order = new CustomerOrder();
        order.setId(8L);
        order.setOrderNo("CAP-8");
        order.setUser(user);
        order.setStatus(OrderStatus.SHIPPED);
        return order;
    }

    private String signature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
