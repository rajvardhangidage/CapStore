package com.capstore.service;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.ProductRepository;
import com.capstore.repository.QuoteRequestRepository;
import com.capstore.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteService {
    private final QuoteRequestRepository quotes;
    private final ProductRepository products;
    private final AuthFacade auth;
    private final NotificationService notifier;

    @Transactional
    public QuoteResponse create(QuoteCreateRequest r) {
        User u = auth.currentUser();
        if (u.getRole() != Role.B2B_USER) throw ApiException.badRequest("Quotes are available for business accounts only");
        QuoteRequest q = new QuoteRequest();
        q.setUser(u);
        if (r.productId() != null)
            q.setProduct(products.findById(r.productId()).orElseThrow(() -> ApiException.badRequest("Invalid product")));
        q.setQuantity(r.quantity());
        q.setDescription(r.description().trim());
        q.setLogoUrl(r.logoUrl());
        return toResponse(quotes.save(q));
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> mine() {
        return quotes.findByUserIdOrderByIdDesc(auth.currentUser().getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public QuoteResponse decide(Long id, boolean accept) {
        QuoteRequest q = quotes.findById(id).filter(x -> x.getUser().getId().equals(auth.currentUser().getId()))
                .orElseThrow(() -> ApiException.notFound("Quote not found"));
        if (q.getStatus() != QuoteStatus.QUOTED) throw ApiException.badRequest("This quote cannot be changed");
        q.setStatus(accept ? QuoteStatus.ACCEPTED : QuoteStatus.REJECTED);
        return toResponse(q);
    }

    // ----- admin -----
    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> adminList(QuoteStatus status, int page, int size) {
        var p = quotes.search(status, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending()));
        return PageResponse.of(p, this::toResponse);
    }

    @Transactional
    public QuoteResponse respond(Long id, QuoteRespondRequest r) {
        QuoteRequest q = quotes.findById(id).orElseThrow(() -> ApiException.notFound("Quote not found"));
        if (q.getStatus() == QuoteStatus.ACCEPTED) throw ApiException.badRequest("Quote already accepted");
        q.setQuotedPrice(r.quotedPrice());
        q.setAdminNote(r.adminNote());
        q.setStatus(QuoteStatus.QUOTED);
        notifier.quoteResponded(q);
        return toResponse(q);
    }

    private QuoteResponse toResponse(QuoteRequest q) {
        User u = q.getUser();
        return new QuoteResponse(q.getId(), q.getProduct() == null ? null : q.getProduct().getId(),
                q.getProduct() == null ? null : q.getProduct().getName(), q.getQuantity(), q.getDescription(),
                q.getLogoUrl(), q.getStatus(), q.getQuotedPrice(), q.getAdminNote(),
                u.getName(), u.getEmail(), u.getCompanyName(), q.getCreatedAt());
    }
}
