package com.capstore;

import com.capstore.dto.OrderDtos.PlaceOrderRequest;
import com.capstore.entity.*;
import com.capstore.repository.*;
import com.capstore.security.AuthFacade;
import com.capstore.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderServiceIdempotencyTest {
    @Test
    void returnsPreviouslyCreatedOrderForRepeatedCheckoutRequest() {
        OrderRepository orders = mock(OrderRepository.class);
        UserRepository users = mock(UserRepository.class);
        AuthFacade auth = new AuthFacade(users);
        User user = new User();
        user.setId(2L);
        user.setName("Customer");
        user.setEmail("customer@example.com");
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.com", "token", AuthorityUtils.NO_AUTHORITIES));
        CustomerOrder previous = new CustomerOrder();
        previous.setId(3L);
        previous.setPublicId("public-id");
        previous.setOrderNo("CAP-3");
        previous.setUser(user);
        previous.setStatus(OrderStatus.PENDING_PAYMENT);
        previous.setPaymentStatus(PaymentStatus.PENDING);
        previous.setSubtotal(BigDecimal.TEN);
        previous.setDiscount(BigDecimal.ZERO);
        previous.setTax(BigDecimal.ZERO);
        previous.setShipping(BigDecimal.ZERO);
        previous.setTotal(BigDecimal.TEN);
        when(orders.findByUserIdAndCheckoutRequestKey(2L, "request-1")).thenReturn(Optional.of(previous));
        CartRepository carts = mock(CartRepository.class);
        OrderService service = new OrderService(orders, carts, mock(ProductVariantRepository.class),
                mock(OrderStatusHistoryRepository.class), new PricingService(),
                new InventoryService(mock(InventoryLogRepository.class)), null, null, null, auth, null);

        var response = service.place(new PlaceOrderRequest("Customer", "1234567890", "Address", null,
                "City", "State", "123456", "request-1", null));

        assertEquals("public-id", response.publicId());
        verifyNoInteractions(carts);
    }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }
}
