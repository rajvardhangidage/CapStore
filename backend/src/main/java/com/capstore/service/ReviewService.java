package com.capstore.service;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.entity.Product;
import com.capstore.entity.Review;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderItemRepository;
import com.capstore.repository.ProductRepository;
import com.capstore.repository.ReviewRepository;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final OrderItemRepository orderItems;
    private final AuthFacade auth;
    private final com.capstore.repository.UserRepository users;

    @Transactional(readOnly = true)
    public ReviewSummary summary(Long productId) {
        List<ReviewResponse> list = reviews.findByProductIdOrderByIdDesc(productId).stream()
                .map(r -> new ReviewResponse(r.getId(), r.getUser().getName(), r.getRating(), r.getBody(), r.getCreatedAt())).toList();
        double avg = list.stream().mapToInt(ReviewResponse::rating).average().orElse(0);
        boolean can = false;
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal())) {
            can = users.findByEmail(a.getName()).map(u -> orderItems.hasPurchased(u.getId(), productId)).orElse(false);
        }
        return new ReviewSummary(Math.round(avg * 10.0) / 10.0, list.size(), can, list);
    }

    @Transactional
    public ReviewResponse upsert(Long productId, ReviewRequest r) {
        User u = auth.currentUser();
        Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!orderItems.hasPurchased(u.getId(), productId))
            throw ApiException.badRequest("Only customers who bought this cap can review it");
        Review rv = reviews.findByUserIdAndProductId(u.getId(), productId).orElseGet(() -> {
            Review n = new Review();
            n.setUser(u);
            n.setProduct(p);
            return n;
        });
        rv.setRating(r.rating());
        rv.setBody(r.body());
        reviews.save(rv);
        return new ReviewResponse(rv.getId(), u.getName(), rv.getRating(), rv.getBody(), rv.getCreatedAt());
    }
}
