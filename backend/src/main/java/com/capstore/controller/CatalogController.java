package com.capstore.controller;

import com.capstore.dto.CatalogDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;

    @GetMapping("/categories")
    public List<CategoryResponse> categories() { return catalog.listCategories(); }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalog.search(categoryId, q, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductResponse product(@PathVariable String id) { return catalog.getPublic(id); }
}
