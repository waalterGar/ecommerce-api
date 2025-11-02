package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.entity.Cart;
import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.entity.Cart;
import com.waalterGar.projects.ecommerce.entity.CartItem;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.mapper.CartMapper;
import com.waalterGar.projects.ecommerce.repository.CartRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.CartService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.utils.Currency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.waalterGar.projects.ecommerce.utils.Currency.EUR;
import static com.waalterGar.projects.ecommerce.utils.Currency.USD;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks CartServiceImpl service;

    private static final String CART_EXT_ID = "cart-123";
    private static final String SKU = "MUG-LOGO-001";
    private static final String NAME = "Logo Mug";
    private static final BigDecimal PRICE = new BigDecimal("19.99");

    // ---------------- create / get ----------------
    @Test
    @DisplayName("createCart: assigns externalId and defaults currency to EUR when null")
    void createCart_defaultsCurrencyAndExternalId() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            if (c.getExternalId() == null) c.setExternalId("generated-ext-id");
            return c;
        });

        CartDto out = service.createCart(null);

        assertThat(out.getExternalId()).isNotBlank();
        assertThat(out.getCurrency()).isEqualTo(EUR);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("getCartByExternalId: returns DTO when cart exists")
    void getCartByExternalId_found_returnsDto() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        addItem(cart, SKU, NAME, PRICE, 2); // 39.98 total

        System.out.println(cart);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));

        CartDto dto = service.getCartByExternalId(CART_EXT_ID);
        System.out.println(dto);

        assertThat(dto.getExternalId()).isEqualTo(CART_EXT_ID);
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getSubtotal()).isEqualByComparingTo("39.98");
        assertThat(dto.getTotal()).isEqualByComparingTo("39.98");
    }

    @Test
    @DisplayName("getCartByExternalId: throws 'Order not found' when missing (as currently implemented)")
    void getCartByExternalId_missing_throwsOrderNotFoundMessage() {
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCartByExternalId(CART_EXT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found"); // matches current implementation
    }

    // ---------------- addItem ----------------
    @Test
    @DisplayName("addItem: adds new item, snapshots price, recomputes totals")
    void addItem_addsNewItem() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(activeProduct(SKU, NAME, PRICE, EUR)));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartDto dto = service.addItem(CART_EXT_ID, SKU, 2);

        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getProductSku()).isEqualTo(SKU);
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(dto.getItems().get(0).getUnitPrice()).isEqualByComparingTo("19.99");
        assertThat(dto.getSubtotal()).isEqualByComparingTo("39.98");
    }

    @Test
    @DisplayName("addItem: increments existing item and refreshes snapshot price")
    void addItem_existing_incrementsQtyAndPrice() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        addItem(cart, SKU, NAME, new BigDecimal("10.00"), 1); // existing
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(activeProduct(SKU, NAME, new BigDecimal("12.34"), EUR)));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartDto dto = service.addItem(CART_EXT_ID, SKU, 2);

        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(dto.getItems().get(0).getUnitPrice()).isEqualByComparingTo("12.34");
        assertThat(dto.getItems().get(0).getLineTotal()).isEqualByComparingTo("37.02");
    }

    @Test
    @DisplayName("addItem: rejects qty <= 0 and empty SKU")
    void addItem_invalidInputs_rejected() {
        assertThatThrownBy(() -> service.addItem(CART_EXT_ID, SKU, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> service.addItem(CART_EXT_ID, "", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    @DisplayName("addItem: inactive product rejected with InactiveProductException")
    void addItem_inactiveProduct_rejected() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));

        Product inactive = activeProduct(SKU, NAME, PRICE, EUR);
        inactive.setIsActive(false);
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.addItem(CART_EXT_ID, SKU, 1))
                .isInstanceOf(InactiveProductException.class)
                .hasMessageContaining("inactive");
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("addItem: currency mismatch is rejected")
    void addItem_currencyMismatch_rejected() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(activeProduct(SKU, NAME, PRICE, USD)));

        assertThatThrownBy(() -> service.addItem(CART_EXT_ID, SKU, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        verify(cartRepository, never()).save(any());
    }

    // ---------------- remove / clear ----------------

    @Test
    @DisplayName("removeItem: removes the item if present (no-op otherwise)")
    void removeItem_removesIfPresent() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        addItem(cart, SKU, NAME, PRICE, 1);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartDto dto = service.removeItem(CART_EXT_ID, SKU);

        assertThat(dto.getItems()).isEmpty();
    }

    @Test
    @DisplayName("removeItem: rejects null/empty SKU")
    void removeItem_invalidSku_rejected() {
        assertThatThrownBy(() -> service.removeItem(CART_EXT_ID, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    @DisplayName("clearCart: removes all items")
    void clearCart_removesAll() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        addItem(cart, SKU, NAME, PRICE, 1);
        addItem(cart, "TSHIRT-LOGO-001", "Logo Tee", new BigDecimal("29.99"), 1);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartDto dto = service.clearCart(CART_EXT_ID);

        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getSubtotal()).isEqualByComparingTo("0.00");
        assertThat(dto.getTotal()).isEqualByComparingTo("0.00");
    }

    // ---------------- updateQty ----------------
    @Test
    @DisplayName("updateQty: qty <= 0 rejected (matches current guard)")
    void updateQty_nonPositive_rejected() {
        assertThatThrownBy(() -> service.updateQty(CART_EXT_ID, SKU, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> service.updateQty(CART_EXT_ID, SKU, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateQty: updates quantity and refreshes snapshot price/name")
    void updateQty_updatesItem() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        addItem(cart, SKU, NAME, new BigDecimal("10.00"), 1);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));

        Product current = activeProduct(SKU, "New Name", new BigDecimal("12.00"), EUR);
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(current));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        CartDto dto = service.updateQty(CART_EXT_ID, SKU, 3);

        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(dto.getItems().get(0).getUnitPrice()).isEqualByComparingTo("12.00");
        assertThat(dto.getItems().get(0).getLineTotal()).isEqualByComparingTo("36.00");
    }

    @Test
    @DisplayName("updateQty: inactive product rejected")
    void updateQty_inactiveProduct_rejected() {
        Cart cart = emptyCart(CART_EXT_ID, EUR);
        addItem(cart, SKU, NAME, PRICE, 1);
        when(cartRepository.findByExternalId(CART_EXT_ID)).thenReturn(Optional.of(cart));

        Product inactive = activeProduct(SKU, NAME, PRICE, EUR);
        inactive.setIsActive(false);
        when(productRepository.findBySku(SKU)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.updateQty(CART_EXT_ID, SKU, 2))
                .isInstanceOf(InactiveProductException.class);
        verify(cartRepository, never()).save(any());
    }

    // ---------------- helpers ----------------
    private static Cart emptyCart(String externalId, Currency currency) {
        Cart c = new Cart();
        c.setId(UUID.randomUUID());
        c.setExternalId(externalId);
        c.setCurrency(currency);
        return c;
    }

    private static void addItem(Cart cart, String sku, String name, BigDecimal unitPrice, int qty) {
        CartItem it = new CartItem();
        it.setProductSku(sku);
        it.setProductName(name);
        it.setUnitPrice(unitPrice);
        it.setQuantity(qty);
        it.computeLineTotal();
        it.setCart(cart);
        cart.getItems().add(it);
    }

    private static Product activeProduct(String sku, String name, BigDecimal price, Currency currency) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setPrice(price);
        p.setCurrency(currency);
        p.setIsActive(true);
        return p;
    }
}