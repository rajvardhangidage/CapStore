package com.capstore.controller;

import com.capstore.service.InvoiceService;
import com.capstore.service.InvoiceService.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoices;

    @GetMapping("/orders/{id}/invoice")
    public ResponseEntity<byte[]> mine(@PathVariable String id) { return pdf(invoices.forCustomer(id)); }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/orders/{id}/invoice")
    public ResponseEntity<byte[]> admin(@PathVariable String id) { return pdf(invoices.forAdmin(id)); }

    private ResponseEntity<byte[]> pdf(Invoice inv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(inv.filename()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(inv.pdf());
    }
}
