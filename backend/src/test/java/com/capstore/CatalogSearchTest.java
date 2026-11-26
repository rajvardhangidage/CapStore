package com.capstore;

import com.capstore.repository.CategoryRepository;
import com.capstore.repository.InventoryLogRepository;
import com.capstore.repository.ProductRepository;
import com.capstore.repository.ProductVariantRepository;
import com.capstore.service.CatalogService;
import com.capstore.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogSearchTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final CatalogService service = new CatalogService(mock(CategoryRepository.class), products,
            mock(ProductVariantRepository.class), new InventoryService(mock(InventoryLogRepository.class)));

    @Test
    void escapesLikeWildcardsInSearchTerms() {
        assertEquals("50!% off", CatalogService.escapeLike("50% off"));
        assertEquals("snap!_back", CatalogService.escapeLike("snap_back"));
        assertEquals("hi!!", CatalogService.escapeLike("hi!"));
    }

    @Test
    void searchPassesEscapedTermAndAddsStableIdTieBreak() {
        when(products.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        service.search(null, "  100%  ", null, null, "price_asc", 0, 12);

        verify(products).search(isNull(), eq("100!%"), isNull(), isNull(), pageable.capture());
        List<Sort.Order> orders = pageable.getValue().getSort().toList();
        assertEquals("basePrice", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        assertEquals("id", orders.get(1).getProperty());
    }

    @Test
    void blankSearchIsIgnoredAndPageSizeIsCapped() {
        when(products.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        service.search(null, "   ", null, null, null, -3, 500);

        verify(products).search(isNull(), q.capture(), isNull(), isNull(), pageable.capture());
        assertNull(q.getValue());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(60, pageable.getValue().getPageSize());
        assertEquals("createdAt", pageable.getValue().getSort().toList().get(0).getProperty());
    }
}
