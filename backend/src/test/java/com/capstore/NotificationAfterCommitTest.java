package com.capstore;

import com.capstore.entity.CustomerOrder;
import com.capstore.entity.OrderStatus;
import com.capstore.entity.User;
import com.capstore.service.NotificationService;
import com.capstore.service.OrderNotificationListener;
import com.capstore.service.OrderStatusMailEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationAfterCommitTest {
    @Test
    void statusChangesPublishMailEventAndListenerRunsAfterCommit() throws Exception {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        NotificationService notifications = new NotificationService(null, events);
        ReflectionTestUtils.setField(notifications, "frontendUrl", "https://shop.example");
        ReflectionTestUtils.setField(notifications, "storeName", "CapStore");
        User user = new User();
        user.setName("Customer");
        user.setEmail("customer@example.com");
        CustomerOrder order = new CustomerOrder();
        order.setId(10L);
        order.setPublicId("opaque-order-id");
        order.setOrderNo("CAP-1");
        order.setStatus(OrderStatus.CANCELLED);
        order.setUser(user);

        notifications.statusChanged(order, "Cancelled");

        var captor = org.mockito.ArgumentCaptor.forClass(OrderStatusMailEvent.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(10L, captor.getValue().orderId());
        assertEquals("ORDER_STATUS_CANCELLED", captor.getValue().type());
        assertEquals("customer@example.com", captor.getValue().recipient());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().body().contains("/orders/opaque-order-id"));
        TransactionalEventListener annotation = OrderNotificationListener.class
                .getMethod("sendStatusMail", OrderStatusMailEvent.class)
                .getAnnotation(TransactionalEventListener.class);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }
}
