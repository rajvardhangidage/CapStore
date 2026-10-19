package com.capstore.service;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.entity.Banner;
import com.capstore.exception.ApiException;
import com.capstore.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository banners;

    @Transactional(readOnly = true)
    public List<BannerResponse> active() { return banners.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(this::toResponse).toList(); }

    @Transactional(readOnly = true)
    public List<BannerResponse> all() { return banners.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toResponse).toList(); }

    @Transactional
    public BannerResponse create(BannerRequest r) { Banner b = new Banner(); apply(b, r); return toResponse(banners.save(b)); }

    @Transactional
    public BannerResponse update(Long id, BannerRequest r) {
        Banner b = banners.findById(id).orElseThrow(() -> ApiException.notFound("Banner not found"));
        apply(b, r);
        return toResponse(b);
    }

    @Transactional
    public void delete(Long id) { banners.deleteById(id); }

    private void apply(Banner b, BannerRequest r) {
        b.setTitle(r.title().trim());
        b.setSubtitle(r.subtitle());
        b.setImageUrl(r.imageUrl());
        b.setLinkUrl(r.linkUrl());
        b.setSortOrder(r.sortOrder());
        if (r.active() != null) b.setActive(r.active());
    }

    private BannerResponse toResponse(Banner b) {
        return new BannerResponse(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), b.getLinkUrl(), b.getSortOrder(), b.isActive());
    }
}
