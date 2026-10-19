package com.capstore.service;

import com.capstore.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {
    private final NotificationLogRepository notificationLogs;
    private final MailService mail;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(OrderNotificationEvent event) {
        String awb = event.awb() == null ? "" : event.awb();
        if (notificationLogs.claim(event.orderId(), event.type(), awb) == 0) {
            log.debug("Skipped duplicate {} notification for order {}", event.type(), event.orderId());
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("customerName", event.customerName());
        model.put("orderNo", event.orderNo());
        model.put("orderId", event.orderId());
        model.put("awb", event.awb());
        model.put("courier", event.courier());
        model.put("trackingUrl", event.trackingUrl());
        model.put("status", event.status());
        model.put("expectedDelivery", event.expectedDelivery());

        String subject;
        String template;
        switch (event.type()) {
            case "ORDER_CONFIRMED" -> {
                subject = "Order confirmed - " + event.orderNo();
                template = "mail/order-confirmed";
            }
            case "SHIPPED" -> {
                subject = "Shipped - order " + event.orderNo();
                template = "mail/order-shipped";
            }
            case "OUT_FOR_DELIVERY", "DELIVERED" -> {
                subject = "Order " + event.orderNo() + " " + event.status().toLowerCase().replace('_', ' ');
                template = "mail/tracking-update";
            }
            case "RETURN_APPROVED", "PICKUP_SCHEDULED", "REFUND_PROCESSED",
                 "RETURN_QC_FAILED", "REPLACEMENT_SHIPPED" -> {
                subject = switch (event.type()) {
                    case "RETURN_APPROVED" -> "Return approved - " + event.orderNo();
                    case "PICKUP_SCHEDULED" -> "Return pickup scheduled - " + event.orderNo();
                    case "REFUND_PROCESSED" -> "Refund processed - " + event.orderNo();
                    case "RETURN_QC_FAILED" -> "Return quality check update - " + event.orderNo();
                    default -> "Replacement shipped - " + event.orderNo();
                };
                template = "mail/return-update";
            }
            default -> {
                log.warn("Ignoring unsupported order notification type {}", event.type());
                return;
            }
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (mail.sendHtml(event.recipient(), subject, template, model)) return;
            log.warn("Notification delivery attempt {} failed for order {}", attempt, event.orderId());
        }
        notificationLogs.release(event.orderId(), event.type(), awb);
        log.error("Giving up notification delivery after retries for order {}", event.orderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendStatusMail(OrderStatusMailEvent event) {
        if (notificationLogs.claim(event.orderId(), event.type(), "") == 0) {
            log.debug("Skipped duplicate {} notification for order {}", event.type(), event.orderId());
            return;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (mail.sendNow(event.recipient(), event.subject(), event.body())) return;
            log.warn("Status email delivery attempt {} failed for {}", attempt, event.recipient());
        }
        notificationLogs.release(event.orderId(), event.type(), "");
        log.error("Giving up status email delivery after retries for {}", event.recipient());
    }
}
