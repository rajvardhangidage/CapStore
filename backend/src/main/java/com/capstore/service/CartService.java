package com.capstore.service;

import com.capstore.dto.CartDtos.*;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.CartRepository;
import com.capstore.repository.ProductVariantRepository;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final PricingService pricing;
    private final AuthFacade auth;

    @Transactional
    public CartResponse get() {
        User u = auth.currentUser();
        return toResponse(findOrCreate(u), u);
    }

    @Transactional
    public CartResponse add(AddItemRequest r) {
        User u = auth.currentUser();
        Cart cart = findOrCreate(u);
        ProductVariant v = variants.findById(r.variantId()).orElseThrow(() -> ApiException.notFound("Variant not found"));
        if (!v.getProduct().isActive()) throw ApiException.badRequest("Product is not available");

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getVariant().getId().equals(v.getId())).findFirst().orElse(null);
        int newQty = (item == null ? 0 : item.getQuantity()) + r.quantity();
        if (newQty > v.getStockQty()) throw ApiException.badRequest("Only " + v.getStockQty() + " in stock");
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(v);
            cart.getItems().add(item);
        }
        item.setQuantity(newQty);
        carts.saveAndFlush(cart);
        return toResponse(cart, u);
    }

    @Transactional
    public CartResponse update(Long itemId, UpdateItemRequest r) {
        User u = auth.currentUser();
        Cart cart = findOrCreate(u);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Cart item not found"));
        if (r.quantity() <= 0) {
            cart.getItems().remove(item);
        } else {
            if (r.quantity() > item.getVariant().getStockQty())
                throw ApiException.badRequest("Only " + item.getVariant().getStockQty() + " in stock");
            item.setQuantity(r.quantity());
        }
        carts.saveAndFlush(cart);
        return toResponse(cart, u);
    }

    @Transactional
    public CartResponse remove(Long itemId) { return update(itemId, new UpdateItemRequest(0)); }

    @Transactional
    public void clear() {
        Cart cart = findOrCreate(auth.currentUser());
        cart.getItems().clear();
    }

    private Cart findOrCreate(User u) {
        return carts.findByUserId(u.getId()).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(u);
            return carts.save(c);
        });
    }

    private CartResponse toResponse(Cart cart, User u) {
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            ProductVariant v = i.getVariant();
            Product p = v.getProduct();
            BigDecimal unit = pricing.unitPrice(v, u, i.getQuantity());
            return new CartItemResponse(i.getId(), v.getId(), p.getId(), p.getPublicId(), p.getName(), v.getSku(), v.getSize(),
                    v.getColor(), p.getImageUrl(), i.getQuantity(), v.getStockQty(), unit,
                    unit.multiply(BigDecimal.valueOf(i.getQuantity())));
        }).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int qty = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(items, qty, subtotal);
    }
}
