package com.capstore.service;

import com.capstore.dto.ExtraDtos.ContactInquiryRequest;
import com.capstore.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactService {
    private final MailService mail;

    @Value("${app.contact.email:}")
    private String contactEmail;

    public void submit(ContactInquiryRequest request) {
        if (contactEmail.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Contact form is temporarily unavailable");
        }

        String body = """
                New website contact inquiry

                Name: %s
                Email: %s
                Phone: %s

                Message:
                %s
                """.formatted(request.name().trim(), request.email().trim(), request.phone().trim(),
                request.message().trim());

        if (!mail.sendContactInquiry(contactEmail, request.email().trim(), body)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "We could not send your message. Please try again later.");
        }
    }
}
