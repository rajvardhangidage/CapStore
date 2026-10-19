package com.capstore.controller;

import com.capstore.dto.AdminDtos.InventoryLogResponse;
import com.capstore.dto.AdminDtos.LowStockItem;
import com.capstore.dto.CatalogDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.service.AdminService;
import com.capstore.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final CatalogService catalog;
    private final AdminService admin;

    // ----- categories -----
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest r) { return catalog.createCategory(r); }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return catalog.updateCategory(id, r);
    }

    // ----- products -----
    @GetMapping("/products")
    public PageResponse<ProductResponse> products(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return catalog.adminList(page, size);
    }

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable String id) { return catalog.adminGet(id); }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest r) { return catalog.create(r); }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest r) { return catalog.update(id, r); }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) { catalog.deactivate(id); return ResponseEntity.noContent().build(); }

    // ----- variants & inventory -----
    @PostMapping("/products/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse addVariant(@PathVariable String id, @Valid @RequestBody VariantRequest r) {
        return catalog.addVariantTo(id, r);
    }

    @PutMapping("/variants/{variantId}")
    public VariantResponse updateVariant(@PathVariable Long variantId, @Valid @RequestBody VariantRequest r) {
        return catalog.updateVariant(variantId, r);
    }

    @PostMapping("/variants/{variantId}/stock")
    public VariantResponse adjustStock(@PathVariable Long variantId, @Valid @RequestBody StockAdjustRequest r) {
        return catalog.adjustStock(variantId, r);
    }

    @GetMapping("/variants/{variantId}/stock-log")
    public PageResponse<InventoryLogResponse> stockLog(@PathVariable Long variantId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return admin.inventoryLog(variantId, page, size);
    }

    @GetMapping("/inventory/low-stock")
    public List<LowStockItem> lowStock() { return admin.lowStock(); }
}
