package com.capstore.controller;

import com.capstore.dto.CartDtos.*;
import com.capstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cart;

    @GetMapping
    public CartResponse get() { return cart.get(); }

    @PostMapping("/items")
    public CartResponse add(@Valid @RequestBody AddItemRequest r) { return cart.add(r); }

    @PutMapping("/items/{itemId}")
    public CartResponse update(@PathVariable Long itemId, @RequestBody UpdateItemRequest r) { return cart.update(itemId, r); }

    @DeleteMapping("/items/{itemId}")
    public CartResponse remove(@PathVariable Long itemId) { return cart.remove(itemId); }

    @DeleteMapping
    public ResponseEntity<Void> clear() { cart.clear(); return ResponseEntity.noContent().build(); }
}
