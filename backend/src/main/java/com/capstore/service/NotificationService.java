package com.capstore.service;

import com.capstore.entity.CustomerOrder;
import com.capstore.entity.QuoteRequest;
import com.capstore.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Builds customer emails. Content is built synchronously (inside the transaction), sending is async. */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final MailService mail;
    private final ApplicationEventPublisher events;

    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${app.store.name}") private String storeName;

    public void statusChanged(CustomerOrder o, String note) {
        StringBuilder sb = new StringBuilder("Hi " + o.getUser().getName() + ",\n\nYour order " + o.getOrderNo()
                + " is now: " + o.getStatus().name().replace('_', ' ') + ".\n");
        if (note != null && !note.isBlank()) sb.append(note).append("\n");
        if (o.getTrackingNo() != null) {
            sb.append("\nCourier: ").append(o.getCourier()).append("\nTracking number: ").append(o.getTrackingNo()).append("\n");
            if (o.getTrackingUrl() != null && !o.getTrackingUrl().isBlank()) sb.append("Track here: ").append(o.getTrackingUrl()).append("\n");
        }
        sb.append("\nOrder details: ").append(frontendUrl).append("/orders/").append(o.getPublicId()).append("\n\n").append(storeName);
        events.publishEvent(new OrderStatusMailEvent(o.getId(), "ORDER_STATUS_" + o.getStatus().name(),
                o.getUser().getEmail(),
                "Order " + o.getOrderNo() + " update", sb.toString()));
    }

    public void passwordReset(String to, String link) {
        mail.send(to, "Reset your password",
                "We received a request to reset your password.\n\nReset it here (valid for 30 minutes):\n" + link
                + "\n\nIf you did not request this, you can ignore this email.\n\n" + storeName);
    }

    public void quoteResponded(QuoteRequest q) {
        mail.send(q.getUser().getEmail(), "Your quote is ready",
                "Hi " + q.getUser().getName() + ",\n\nWe have quoted Rs. " + q.getQuotedPrice() + " per piece for "
                + q.getQuantity() + " pcs." + (q.getAdminNote() != null ? "\nNote: " + q.getAdminNote() : "")
                + "\n\nAccept or decline here: " + frontendUrl + "/quotes\n\n" + storeName);
    }

    public void businessApproved(User u) {
        mail.send(u.getEmail(), "Your business account is approved",
                "Hi " + u.getName() + ",\n\nYour business account for " + u.getCompanyName()
                + " is approved. Bulk pricing is now active when you log in.\n\n" + frontendUrl + "\n\n" + storeName);
    }
}
