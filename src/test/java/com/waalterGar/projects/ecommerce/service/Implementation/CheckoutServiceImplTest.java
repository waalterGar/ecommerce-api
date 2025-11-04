package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.waalterGar.projects.ecommerce.Dto.CheckoutRequestDto;
import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Cart;
import com.waalterGar.projects.ecommerce.entity.CartItem;
import com.waalterGar.projects.ecommerce.repository.CartRepository;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import com.waalterGar.projects.ecommerce.utils.CartStatus;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CheckoutServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock OrderService orderService;

    @InjectMocks CheckoutServiceImpl checkoutService;

    private static final String CART_ID = "cart-123";
    private static final String CUSTOMER_ID = "cust-999";
    private static final String SKU1 = "MUG-LOGO-001";
    private static final String SKU2 = "TSHIRT-LOGO-001";

    // ---------- Happy path ----------

    @Test
    @DisplayName("checkout: delegates to OrderService, marks cart CHECKED_OUT, returns OrderDto")
    void checkout_happyPath_delegates_and_marksCart() {
        // Given a cart with two items
        Cart cart = cartWithItems(CART_ID, Currency.EUR,
                line(SKU1, 2),
                line(SKU2, 1)
        );
        when(cartRepository.findByExternalId(CART_ID)).thenReturn(Optional.of(cart));

        // Capture the createOrderDto sent to OrderService
        ArgumentCaptor<createOrderDto> dtoCap = ArgumentCaptor.forClass(createOrderDto.class);

        // Simulate OrderService producing an OrderDto
        OrderDto created = new OrderDto();
        created.setExternalId("ord-abc");
        when(orderService.createOrder(dtoCap.capture())).thenReturn(created);

        // Request payload
        CheckoutRequestDto body = new CheckoutRequestDto();
        body.setCustomerExternalId(CUSTOMER_ID);

        // When
        OrderDto out = checkoutService.checkout(CART_ID, CUSTOMER_ID);

        // Then: service delegates with correct items & customer
        createOrderDto sent = dtoCap.getValue();
        assertThat(sent.getCustomerExternalId()).isEqualTo(CUSTOMER_ID);
        assertThat(sent.getItems()).extracting(createOrderItemDto::getProductSku)
                .containsExactlyInAnyOrder(SKU1, SKU2);
        assertThat(sent.getItems()).extracting(createOrderItemDto::getQuantity)
                .containsExactlyInAnyOrder(2, 1);

        // Cart is marked checked out and saved
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(cart.getCheckedOutAt()).isNotNull();
        verify(cartRepository).save(cart);

        // Response is the order returned by OrderService
        assertThat(out.getExternalId()).isEqualTo("ord-abc");

        verify(orderService).createOrder(any(createOrderDto.class));
        verifyNoMoreInteractions(orderService);
    }

    // ---------- Guards / Errors ----------

    @Test
    @DisplayName("checkout: cart not found -> NoSuchElementException('Cart not found')")
    void checkout_cartNotFound_throws404() {
        when(cartRepository.findByExternalId(CART_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, CUSTOMER_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Cart not found");

        verifyNoInteractions(orderService);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: empty cart -> IllegalArgumentException('Cart is empty')")
    void checkout_emptyCart_throws400() {
        Cart empty = new Cart();
        empty.setExternalId(CART_ID);
        empty.setStatus(CartStatus.NEW);
        empty.setCurrency(Currency.EUR);
        when(cartRepository.findByExternalId(CART_ID)).thenReturn(java.util.Optional.of(empty));

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, CUSTOMER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart is empty");

        verifyNoInteractions(orderService);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: cart already CHECKED_OUT -> IllegalStateException")
    void checkout_alreadyCheckedOut_throws() {
        Cart checked = cartWithItems(CART_ID, Currency.EUR, line(SKU1, 1));
        checked.setStatus(CartStatus.CHECKED_OUT);
        when(cartRepository.findByExternalId(CART_ID)).thenReturn(java.util.Optional.of(checked));

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, CUSTOMER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already checked out");

        verifyNoInteractions(orderService);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: missing customerExternalId -> IllegalArgumentException")
    void checkout_missingCustomer_throws400() {
        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerExternalId is required");

        verifyNoInteractions(orderService);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: propagates InactiveProductException from OrderService and does not mark cart")
    void checkout_inactiveProduct_propagates_andNoSave() {
        Cart cart = cartWithItems(CART_ID, Currency.EUR, line(SKU1, 1));
        when(cartRepository.findByExternalId(CART_ID)).thenReturn(java.util.Optional.of(cart));

        when(orderService.createOrder(any(createOrderDto.class)))
                .thenThrow(new InactiveProductException("Product is inactive: " + SKU1));

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, CUSTOMER_ID))
                .isInstanceOf(InactiveProductException.class);

        // Cart status must NOT be changed/saved on failure before delegation returns
        assertThat(cart.getStatus()).isEqualTo(CartStatus.NEW);
        assertThat(cart.getCheckedOutAt()).isNull();
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: propagates InsufficientStockException from OrderService and does not mark cart")
    void checkout_outOfStock_propagates_andNoSave() {
        Cart cart = cartWithItems(CART_ID, Currency.EUR, line(SKU1, 3));
        when(cartRepository.findByExternalId(CART_ID)).thenReturn(java.util.Optional.of(cart));

        when(orderService.createOrder(any(createOrderDto.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        assertThatThrownBy(() -> checkoutService.checkout(CART_ID, CUSTOMER_ID))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(cart.getStatus()).isEqualTo(CartStatus.NEW);
        assertThat(cart.getCheckedOutAt()).isNull();
        verify(cartRepository, never()).save(any());
    }

    // ---------- Helpers ----------

    private static CheckoutRequestDto req(String customerExternalId) {
        CheckoutRequestDto r = new CheckoutRequestDto();
        r.setCustomerExternalId(customerExternalId);
        return r;
    }

    private static Cart cartWithItems(String externalId, Currency currency, CartItem... items) {
        Cart c = new Cart();
        c.setId(UUID.randomUUID());
        c.setExternalId(externalId);
        c.setCurrency(currency);
        c.setStatus(CartStatus.NEW);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        c.setItems(new java.util.ArrayList<>());
        for (CartItem it : items) {
            it.setCart(c);
            c.getItems().add(it);
        }
        return c;
    }

    private static CartItem line(String sku, int qty) {
        CartItem ci = new CartItem();
        ci.setProductSku(sku);
        ci.setProductName("n/a");
        ci.setQuantity(qty);
        // price snapshot is irrelevant here; OrderService re-reads products and sets prices
        return ci;
    }
}
