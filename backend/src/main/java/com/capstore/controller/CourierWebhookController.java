package com.capstore.controller;

import com.capstore.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/courier")
@RequiredArgsConstructor
public class CourierWebhookController {
    private final TrackingService tracking;

    @PostMapping("/{courierName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String courierName,
                       @RequestHeader(value = "X-Signature", required = false) String signature,
                       @RequestBody byte[] rawBody) {
        tracking.handleWebhook(courierName, rawBody, signature);
    }
}
