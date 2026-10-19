package com.capstore.service;

import com.capstore.dto.CatalogDtos.ProductResponse;
import com.capstore.entity.Product;
import com.capstore.entity.User;
import com.capstore.entity.WishlistItem;
import com.capstore.exception.ApiException;
import com.capstore.repository.ProductRepository;
import com.capstore.repository.WishlistRepository;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlist;
    private final ProductRepository products;
    private final CatalogService catalog;
    private final AuthFacade auth;

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return wishlist.findByUserIdOrderByIdDesc(auth.currentUser().getId()).stream()
                .map(WishlistItem::getProduct).filter(Product::isActive).map(catalog::toResponse).toList();
    }

    @Transactional
    public void add(Long productId) {
        User u = auth.currentUser();
        if (wishlist.findByUserIdAndProductId(u.getId(), productId).isPresent()) return;
        Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"));
        WishlistItem w = new WishlistItem();
        w.setUser(u);
        w.setProduct(p);
        wishlist.save(w);
    }

    @Transactional
    public void remove(Long productId) {
        wishlist.findByUserIdAndProductId(auth.currentUser().getId(), productId).ifPresent(wishlist::delete);
    }
}
