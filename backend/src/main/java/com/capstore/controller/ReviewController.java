package com.capstore.controller;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviews;

    @GetMapping
    public ReviewSummary list(@PathVariable Long productId) { return reviews.summary(productId); }

    @PostMapping
    public ReviewResponse save(@PathVariable Long productId, @Valid @RequestBody ReviewRequest r) { return reviews.upsert(productId, r); }
}
