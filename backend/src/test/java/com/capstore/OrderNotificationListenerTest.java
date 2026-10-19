package com.capstore;

import com.capstore.repository.NotificationLogRepository;
import com.capstore.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.thymeleaf.TemplateEngine;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderNotificationListenerTest {
    @Test
    void retriesTransientHtmlMailFailureAndKeepsSuccessfulClaim() {
        NotificationLogRepository logs = mock(NotificationLogRepository.class);
        when(logs.claim(4L, "ORDER_CONFIRMED", "")).thenReturn(1);
        RetryMail mail = new RetryMail(false, true);
        OrderNotificationListener listener = new OrderNotificationListener(logs, mail);
        OrderNotificationEvent event = new OrderNotificationEvent(4L, "ORDER_CONFIRMED",
                "customer@example.com", "Customer", "CAP-4", null, null, "/orders/public",
                null, null);

        listener.send(event);

        assertEquals(2, mail.attempts);
        verify(logs, never()).release(4L, "ORDER_CONFIRMED", "");
    }

    @Test
    void releasesNotificationClaimWhenAllDeliveryAttemptsFail() {
        NotificationLogRepository logs = mock(NotificationLogRepository.class);
        when(logs.claim(4L, "ORDER_CONFIRMED", "")).thenReturn(1);
        RetryMail mail = new RetryMail(false, false, false);
        OrderNotificationListener listener = new OrderNotificationListener(logs, mail);
        OrderNotificationEvent event = new OrderNotificationEvent(4L, "ORDER_CONFIRMED",
                "customer@example.com", "Customer", "CAP-4", null, null, "/orders/public",
                null, null);

        listener.send(event);

        assertEquals(3, mail.attempts);
        verify(logs).release(4L, "ORDER_CONFIRMED", "");
    }

    @Test
    void sendsStatusMailOnlyThroughAfterCommitListenerAndRetries() {
        NotificationLogRepository logs = mock(NotificationLogRepository.class);
        when(logs.claim(4L, "ORDER_STATUS_CANCELLED", "")).thenReturn(1);
        RetryMail mail = new RetryMail(false, true);
        OrderNotificationListener listener = new OrderNotificationListener(logs, mail);

        listener.sendStatusMail(new OrderStatusMailEvent(4L, "ORDER_STATUS_CANCELLED",
                "customer@example.com", "Order update", "body"));

        assertEquals(2, mail.statusAttempts);
        verify(logs, never()).release(4L, "ORDER_STATUS_CANCELLED", "");
    }

    @Test
    void skipsDuplicateStatusNotificationClaims() {
        NotificationLogRepository logs = mock(NotificationLogRepository.class);
        RetryMail mail = new RetryMail(true);
        OrderNotificationListener listener = new OrderNotificationListener(logs, mail);

        listener.sendStatusMail(new OrderStatusMailEvent(4L, "ORDER_STATUS_CANCELLED",
                "customer@example.com", "Order update", "body"));

        assertEquals(0, mail.statusAttempts);
    }

    private static final class RetryMail extends MailService {
        private final boolean[] results;
        private int attempts;
        private int statusAttempts;

        private RetryMail(boolean... results) {
            super((ObjectProvider<JavaMailSender>) null, "from@example.com", (TemplateEngine) null);
            this.results = results;
        }

        @Override
        public boolean sendHtml(String to, String subject, String template, Map<String, Object> model) {
            return results[attempts++];
        }

        @Override
        public boolean sendNow(String to, String subject, String body) {
            return results[statusAttempts++];
        }
    }
}
