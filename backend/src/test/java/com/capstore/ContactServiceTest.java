package com.capstore;

import com.capstore.dto.ExtraDtos.ContactInquiryRequest;
import com.capstore.exception.ApiException;
import com.capstore.service.ContactService;
import com.capstore.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContactServiceTest {
    @Test
    void forwardsValidatedInquiryToTheConfiguredInbox() {
        StubMail mail = new StubMail(true);
        ContactService service = new ContactService(mail);
        ReflectionTestUtils.setField(service, "contactEmail", "support@example.com");

        service.submit(new ContactInquiryRequest("Customer", "customer@example.com", "+91 98765 43210",
                "I need an update on Order #42."));

        assertEquals("support@example.com", mail.to);
        assertEquals("customer@example.com", mail.replyTo);
        org.junit.jupiter.api.Assertions.assertTrue(mail.body.contains("Phone: +91 98765 43210"));
        org.junit.jupiter.api.Assertions.assertTrue(mail.body.contains("Order #42"));
    }

    @Test
    void reportsUnavailableWhenContactEmailIsNotConfigured() {
        ContactService service = new ContactService(new StubMail(true));
        ReflectionTestUtils.setField(service, "contactEmail", " ");

        ApiException error = assertThrows(ApiException.class, () -> service.submit(
                new ContactInquiryRequest("Customer", "customer@example.com", "+91 98765 43210",
                        "I need help with an order.")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    @Test
    void reportsUnavailableWhenEmailCannotBeSent() {
        StubMail mail = new StubMail(false);
        ContactService service = new ContactService(mail);
        ReflectionTestUtils.setField(service, "contactEmail", "support@example.com");

        ApiException error = assertThrows(ApiException.class, () -> service.submit(
                new ContactInquiryRequest("Customer", "customer@example.com", "+91 98765 43210",
                        "I need help with an order.")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    private static final class StubMail extends MailService {
        private final boolean result;
        private String to;
        private String replyTo;
        private String body;

        private StubMail(boolean result) {
            super((ObjectProvider<JavaMailSender>) null, "from@example.com", (TemplateEngine) null);
            this.result = result;
        }

        @Override
        public boolean sendContactInquiry(String to, String replyTo, String body) {
            this.to = to;
            this.replyTo = replyTo;
            this.body = body;
            return result;
        }
    }
}
