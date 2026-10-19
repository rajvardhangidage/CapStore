package com.capstore.service;

import com.capstore.dto.CatalogDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.*;
import com.capstore.exception.ApiException;
import com.capstore.repository.CategoryRepository;
import com.capstore.repository.ProductRepository;
import com.capstore.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final InventoryService inventory;

    // ---------- categories ----------
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categories.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest r) {
        Category c = new Category();
        c.setName(r.name().trim());
        c.setSlug(uniqueSlug(r.name(), categories::existsBySlug));
        c.setParentId(r.parentId());
        return toResponse(categories.save(c));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest r) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
        c.setName(r.name().trim());
        c.setParentId(r.parentId());
        return toResponse(c);
    }

    // ---------- public product browsing ----------
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(Long categoryId, String q, BigDecimal min, BigDecimal max,
                                                String sort, int page, int size) {
        Sort s = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            default -> Sort.by("createdAt").descending();
        };
        String query = (q == null || q.isBlank()) ? null : q.trim();
        var result = products.search(categoryId, query, min, max, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 60), s));
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublic(String publicId) {
        Product p = products.findByPublicIdAndActiveTrue(publicId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        return toResponse(p);
    }

    // ---------- admin ----------
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> adminList(int page, int size) {
        var result = products.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending()));
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse adminGet(String publicId) {
        return toResponse(findByPublicId(publicId));
    }

    @Transactional
    public ProductResponse create(ProductRequest r) {
        Product p = new Product();
        p.setSlug(uniqueSlug(r.name(), products::existsBySlug));
        apply(p, r);
        if (r.variants() != null) {
            for (VariantRequest vr : r.variants()) addVariant(p, vr);
        }
        return toResponse(products.save(p));
    }

    @Transactional
    public ProductResponse update(String publicId, ProductRequest r) {
        Product p = findByPublicId(publicId);
        apply(p, r);
        return toResponse(p);
    }

    @Transactional
    public void deactivate(String publicId) {
        Product p = findByPublicId(publicId);
        p.setActive(false);
    }

    @Transactional
    public ProductResponse addVariantTo(String publicId, VariantRequest vr) {
        Product p = findByPublicId(publicId);
        addVariant(p, vr);
        products.saveAndFlush(p);
        return toResponse(p);
    }

    @Transactional
    public VariantResponse updateVariant(Long variantId, VariantRequest vr) {
        ProductVariant v = variants.findById(variantId).orElseThrow(() -> ApiException.notFound("Variant not found"));
        if (!v.getSku().equals(vr.sku()) && variants.existsBySku(vr.sku()))
            throw ApiException.conflict("SKU already exists");
        v.setSku(vr.sku().trim());
        v.setSize(vr.size());
        v.setColor(vr.color());
        v.setPrice(vr.price());
        return toResponse(v);
    }

    @Transactional
    public VariantResponse adjustStock(Long variantId, StockAdjustRequest r) {
        ProductVariant v = variants.findByIdForUpdate(variantId)
                .orElseThrow(() -> ApiException.notFound("Variant not found"));
        inventory.adjust(v, r.delta(), "ADMIN_" + r.reason().trim().toUpperCase().replace(' ', '_'), null);
        return toResponse(v);
    }

    // ---------- helpers ----------
    private void apply(Product p, ProductRequest r) {
        Category c = categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("Invalid category"));
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setCategory(c);
        p.setBasePrice(r.basePrice());
        if (r.imageUrls() != null) {
            List<String> images = r.imageUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .distinct()
                    .toList();
            p.getImageUrls().clear();
            p.getImageUrls().addAll(images);
            p.setImageUrl(images.isEmpty() ? null : images.get(0));
        } else {
            p.setImageUrl(r.imageUrl());
            p.getImageUrls().clear();
            if (r.imageUrl() != null && !r.imageUrl().isBlank()) p.getImageUrls().add(r.imageUrl());
        }
        if (r.active() != null) p.setActive(r.active());
        if (r.returnable() != null) p.setReturnable(r.returnable());
        if (r.tiers() != null) {
            p.getTiers().clear();
            for (TierRequest t : r.tiers()) {
                BulkPriceTier bt = new BulkPriceTier();
                bt.setProduct(p);
                bt.setMinQty(t.minQty());
                bt.setPrice(t.price());
                p.getTiers().add(bt);
            }
        }
    }

    private void addVariant(Product p, VariantRequest vr) {
        if (variants.existsBySku(vr.sku().trim())) throw ApiException.conflict("SKU already exists: " + vr.sku());
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setSku(vr.sku().trim());
        v.setSize(vr.size());
        v.setColor(vr.color());
        v.setPrice(vr.price());
        v.setStockQty(vr.stockQty());
        p.getVariants().add(v);
    }

    private Product findByPublicId(String publicId) {
        return products.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    private interface Exists { boolean test(String slug); }

    private String uniqueSlug(String name, Exists exists) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "item";
        String slug = base;
        int i = 2;
        while (exists.test(slug)) slug = base + "-" + i++;
        return slug;
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getParentId());
    }

    private VariantResponse toResponse(ProductVariant v) {
        return new VariantResponse(v.getId(), v.getSku(), v.getSize(), v.getColor(), v.getPrice(), v.getStockQty());
    }

    public ProductResponse toResponse(Product p) {
        List<String> images = p.getImageUrls().isEmpty() && p.getImageUrl() != null
                ? List.of(p.getImageUrl())
                : List.copyOf(p.getImageUrls());
        return new ProductResponse(p.getId(), p.getPublicId(), p.getName(), p.getSlug(), p.getDescription(),
                p.getCategory().getId(), p.getCategory().getName(), p.getBasePrice(), p.getImageUrl(), p.isActive(),
                images,
                p.getVariants().stream().map(this::toResponse).toList(),
                p.getTiers().stream().sorted(Comparator.comparingInt(BulkPriceTier::getMinQty))
                        .map(t -> new TierResponse(t.getMinQty(), t.getPrice())).toList(),
                p.isReturnable());
    }
}
