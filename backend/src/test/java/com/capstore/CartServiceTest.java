package com.capstore;

import com.capstore.dto.CartDtos.AddItemRequest;
import com.capstore.dto.CartDtos.UpdateItemRequest;
import com.capstore.entity.Cart;
import com.capstore.entity.CartItem;
import com.capstore.entity.Product;
import com.capstore.entity.ProductVariant;
import com.capstore.entity.Role;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.CartRepository;
import com.capstore.repository.ProductVariantRepository;
import com.capstore.repository.UserRepository;
import com.capstore.security.AuthFacade;
import com.capstore.service.CartService;
import com.capstore.service.PricingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private static final Long USER_ID = 1L;

    private final CartRepository carts = mock(CartRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuthFacade auth = new AuthFacade(users);
    private CartService service;
    private User user;

    @BeforeEach
    void setUp() {
        user = user();
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "customer@example.com", "token", AuthorityUtils.NO_AUTHORITIES));
        service = new CartService(carts, variants, new PricingService(), auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addingItemReturnsPricedLineAndCartTotals() {
        Cart cart = cart();
        ProductVariant variant = variant(10L, 5, true);
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(variants.findById(10L)).thenReturn(Optional.of(variant));

        var response = service.add(new AddItemRequest(10L, 2));

        assertEquals(2, response.totalQuantity());
        assertEquals(new BigDecimal("25.00"), response.subtotal());
        assertEquals(1, response.items().size());
        assertEquals(variant.getId(), response.items().get(0).variantId());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("12.50"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("25.00"), response.items().get(0).lineTotal());
        verify(carts).saveAndFlush(cart);
    }

    @Test
    void addingExistingVariantIncreasesItsQuantity() {
        Cart cart = cart();
        ProductVariant variant = variant(10L, 5, true);
        CartItem item = cartItem(cart, variant, 2);
        cart.getItems().add(item);
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(variants.findById(10L)).thenReturn(Optional.of(variant));

        var response = service.add(new AddItemRequest(10L, 2));

        assertEquals(1, cart.getItems().size());
        assertEquals(4, item.getQuantity());
        assertEquals(4, response.totalQuantity());
        assertEquals(new BigDecimal("50.00"), response.subtotal());
    }

    @Test
    void addingMoreThanAvailableStockFailsWithoutSaving() {
        Cart cart = cart();
        ProductVariant variant = variant(10L, 3, true);
        CartItem item = cartItem(cart, variant, 2);
        cart.getItems().add(item);
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(variants.findById(10L)).thenReturn(Optional.of(variant));

        ApiException error = assertThrows(ApiException.class,
                () -> service.add(new AddItemRequest(10L, 2)));

        assertEquals(400, error.getStatus().value());
        assertEquals(2, item.getQuantity());
        verify(carts, never()).saveAndFlush(any());
    }

    @Test
    void addingAQuantityThatWouldOverflowIsRejected() {
        Cart cart = cart();
        ProductVariant variant = variant(10L, 5, true);
        CartItem item = cartItem(cart, variant, 2);
        cart.getItems().add(item);
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(variants.findById(10L)).thenReturn(Optional.of(variant));

        assertThrows(ApiException.class, () -> service.add(new AddItemRequest(10L, Integer.MAX_VALUE)));

        assertEquals(2, item.getQuantity());
        verify(carts, never()).saveAndFlush(any());
    }

    @Test
    void addingUnavailableProductFails() {
        Cart cart = cart();
        ProductVariant variant = variant(10L, 5, false);
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(variants.findById(10L)).thenReturn(Optional.of(variant));

        ApiException error = assertThrows(ApiException.class,
                () -> service.add(new AddItemRequest(10L, 1)));

        assertEquals(400, error.getStatus().value());
        verify(carts, never()).saveAndFlush(any());
    }

    @Test
    void updatingQuantityRepricesTheCartAndZeroRemovesTheItem() {
        Cart cart = cart();
        ProductVariant variant = variant(10L, 5, true);
        CartItem item = cartItem(cart, variant, 1);
        item.setId(20L);
        cart.getItems().add(item);
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        var updated = service.update(20L, new UpdateItemRequest(3));

        assertEquals(3, item.getQuantity());
        assertEquals(new BigDecimal("37.50"), updated.subtotal());

        var removed = service.update(20L, new UpdateItemRequest(0));

        assertEquals(0, cart.getItems().size());
        assertEquals(0, removed.totalQuantity());
        assertEquals(BigDecimal.ZERO, removed.subtotal());
        verify(carts, times(2)).saveAndFlush(cart);
    }

    @Test
    void missingVariantAndCartItemReturnNotFound() {
        Cart cart = cart();
        when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(variants.findById(99L)).thenReturn(Optional.empty());

        ApiException variantError = assertThrows(ApiException.class,
                () -> service.add(new AddItemRequest(99L, 1)));
        ApiException itemError = assertThrows(ApiException.class,
                () -> service.update(99L, new UpdateItemRequest(1)));

        assertEquals(404, variantError.getStatus().value());
        assertEquals(404, itemError.getStatus().value());
        verify(carts, never()).saveAndFlush(any());
    }

    private static User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("customer@example.com");
        user.setRole(Role.USER);
        return user;
    }

    private static Cart cart() {
        Cart cart = new Cart();
        cart.setItems(new java.util.ArrayList<>());
        return cart;
    }

    private static ProductVariant variant(Long id, int stock, boolean active) {
        Product product = new Product();
        product.setId(100L);
        product.setPublicId("product-100");
        product.setName("Test product");
        product.setActive(active);
        product.setBasePrice(new BigDecimal("12.50"));

        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setSku("SKU-" + id);
        variant.setStockQty(stock);
        return variant;
    }

    private static CartItem cartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }
}
