package com.capstore.controller;

import com.capstore.dto.AdminDtos.CustomerResponse;
import com.capstore.dto.ExtraDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.QuoteStatus;
import com.capstore.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminExtrasController {
    private final CouponService coupons;
    private final BannerService banners;
    private final QuoteService quotes;
    private final ReportService reports;
    private final AdminUserService adminUsers;
    private final StoreSettingsService storeSettings;

    // ----- store settings -----
    @GetMapping("/store-settings")
    public StoreConfig storeSettings() { return storeSettings.get(); }

    @PutMapping("/store-settings")
    public StoreConfig updateStoreSettings(@Valid @RequestBody StoreSettingsRequest request) {
        return storeSettings.update(request);
    }

    // ----- coupons -----
    @GetMapping("/coupons")
    public List<CouponResponse> coupons() { return coupons.list(); }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(@Valid @RequestBody CouponRequest r) { return coupons.create(r); }

    @PutMapping("/coupons/{id}")
    public CouponResponse updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest r) { return coupons.update(id, r); }

    // ----- banners -----
    @GetMapping("/banners")
    public List<BannerResponse> banners() { return banners.all(); }

    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public BannerResponse createBanner(@Valid @RequestBody BannerRequest r) { return banners.create(r); }

    @PutMapping("/banners/{id}")
    public BannerResponse updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest r) { return banners.update(id, r); }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) { banners.delete(id); return ResponseEntity.noContent().build(); }

    // ----- quotes -----
    @GetMapping("/quotes")
    public PageResponse<QuoteResponse> quotes(@RequestParam(required = false) QuoteStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return quotes.adminList(status, page, size);
    }

    @PutMapping("/quotes/{id}/respond")
    public QuoteResponse respond(@PathVariable Long id, @Valid @RequestBody QuoteRespondRequest r) { return quotes.respond(id, r); }

    // ----- reports -----
    @GetMapping("/reports/{type}")
    public ReportData report(@PathVariable String type,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.build(type, from, to);
    }

    @GetMapping("/reports/{type}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] xlsx = reports.toXlsx(reports.build(type, from, to));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(type + "-report.xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    // ----- admin users -----
    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createAdmin(@Valid @RequestBody CreateAdminRequest r) { return adminUsers.createAdmin(r); }
}
