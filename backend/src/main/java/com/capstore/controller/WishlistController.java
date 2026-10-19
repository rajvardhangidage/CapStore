package com.capstore.controller;

import com.capstore.dto.CatalogDtos.ProductResponse;
import com.capstore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlist;

    @GetMapping
    public List<ProductResponse> list() { return wishlist.list(); }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> add(@PathVariable Long productId) { wishlist.add(productId); return ResponseEntity.noContent().build(); }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@PathVariable Long productId) { wishlist.remove(productId); return ResponseEntity.noContent().build(); }
}
