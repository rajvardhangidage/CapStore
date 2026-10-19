package com.capstore.controller;

import com.capstore.dto.ExtraDtos.ContactInquiryRequest;
import com.capstore.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> submit(@Valid @RequestBody ContactInquiryRequest request) {
        contactService.submit(request);
        return Map.of("message", "Thanks for contacting us. Your message has been sent.");
    }
}
