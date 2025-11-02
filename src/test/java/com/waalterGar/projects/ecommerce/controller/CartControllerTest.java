package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.Dto.CartItemDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.service.CartService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class CartControllerTest {
    private static final String BASE_URL = "/carts";
    private static final String CART_ID = "cart-123";
    private static final String SKU = "MUG-LOGO-001";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    CartService cartService;

    @Test
    @DisplayName("POST /carts -> 200 with new cart")
    void createCart_returns200_withBody() throws Exception {
        when(cartService.createCart(null)).thenReturn(emptyCart(CART_ID, Currency.EUR));

        mvc.perform(post(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value(CART_ID))
                .andExpect(handler().handlerType(CartController.class))
                .andExpect(handler().methodName("createCart"));

        verify(cartService).createCart(null);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("GET /carts/{id} -> 200 with cart body")
    void getCart_returns200_withBody() throws Exception {
        when(cartService.getCartByExternalId(CART_ID))
                .thenReturn(cartWithOneItem(CART_ID, SKU, "Logo Mug", "19.99", 2));

        mvc.perform(get(BASE_URL + "/" + CART_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value(CART_ID))
                .andExpect(jsonPath("$.items[0].productSku").value(SKU))
                .andExpect(jsonPath("$.subtotal").value(39.98))
                .andExpect(handler().methodName("getCart"));

        verify(cartService).getCartByExternalId(CART_ID);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("GET /carts/{id} -> 404 ProblemDetail when missing")
    void getCart_missing_returns404_problem() throws Exception {
        when(cartService.getCartByExternalId(CART_ID))
                .thenThrow(new NoSuchElementException("Cart not found"));

        mvc.perform(get(BASE_URL + "/" + CART_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        verify(cartService).getCartByExternalId(CART_ID);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /carts/{id}/items -> 200 adds item and returns cart")
    void addItem_valid_returns200() throws Exception {
        String payload = """
        { "sku": "MUG-LOGO-001", "qty": 2 }
        """;
        when(cartService.addItem(eq(CART_ID), eq(SKU), eq(2)))
                .thenReturn(cartWithOneItem(CART_ID, SKU, "Logo Mug", "19.99", 2));

        mvc.perform(post(BASE_URL + "/" + CART_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productSku").value(SKU))
                .andExpect(jsonPath("$.subtotal").value(39.98))
                .andExpect(handler().methodName("addItem"));

        verify(cartService).addItem(CART_ID, SKU, 2);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /carts/{id}/items -> 400 ProblemDetail on invalid payload")
    void addItem_invalidPayload_returns400_problem() throws Exception {
        // blank sku, qty <= 0
        String bad = """
        { "sku": "", "qty": 0 }
        """;

        mvc.perform(post(BASE_URL + "/" + CART_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"))
                .andExpect(jsonPath("$.title").value("Validation Failed"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /carts/{id}/items -> 422 ProblemDetail when product inactive")
    void addItem_inactive_returns422_problem() throws Exception {
        String payload = """
        { "sku": "MUG-LOGO-001", "qty": 1 }
        """;
        when(cartService.addItem(eq(CART_ID), eq(SKU), eq(1)))
                .thenThrow(new InactiveProductException("Product is inactive: " + SKU));

        mvc.perform(post(BASE_URL + "/" + CART_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:inactive-product"))
                .andExpect(jsonPath("$.title").value("Inactive Product"));

        verify(cartService).addItem(CART_ID, SKU, 1);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("PUT /carts/{id}/items/{sku} -> 200 updates quantity")
    void updateQty_valid_returns200() throws Exception {
        String payload = """
        { "qty": 3 }
        """;
        when(cartService.updateQty(eq(CART_ID), eq(SKU), eq(3)))
                .thenReturn(cartWithOneItem(CART_ID, SKU, "Logo Mug", "10.00", 3));

        mvc.perform(put(BASE_URL + "/" + CART_ID + "/items/" + SKU)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(handler().methodName("updateQty"));

        verify(cartService).updateQty(CART_ID, SKU, 3);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("PUT /carts/{id}/items/{sku} -> 400 ProblemDetail on invalid qty")
    void updateQty_invalidQty_returns400_problem() throws Exception {
        String bad = """
        { "qty": 0 }
        """;

        mvc.perform(put(BASE_URL + "/" + CART_ID + "/items/" + SKU)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"))
                .andExpect(jsonPath("$.title").value("Validation Failed"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("DELETE /carts/{id}/items/{sku} -> 200 removes item")
    void removeItem_returns200() throws Exception {
        when(cartService.removeItem(eq(CART_ID), eq(SKU)))
                .thenReturn(emptyCart(CART_ID, Currency.EUR));

        mvc.perform(delete(BASE_URL + "/" + CART_ID + "/items/" + SKU)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(handler().methodName("removeItem"));

        verify(cartService).removeItem(CART_ID, SKU);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("DELETE /carts/{id}/items -> 200 clears all items")
    void clearCart_returns200() throws Exception {
        when(cartService.clearCart(eq(CART_ID)))
                .thenReturn(emptyCart(CART_ID, Currency.EUR));

        mvc.perform(delete(BASE_URL + "/" + CART_ID + "/items")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(handler().methodName("clearCart"));

        verify(cartService).clearCart(CART_ID);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("POST /carts/{id}/items -> 415 when unsupported media type")
    void addItem_unsupportedMediaType_returns415_problem() throws Exception {
        mvc.perform(post(BASE_URL + "/" + CART_ID + "/items")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("sku=MUG-LOGO-001&qty=1"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:unsupported-media-type"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /carts/{id}/items -> 400 when malformed JSON")
    void addItem_malformedJson_returns400_problem() throws Exception {
        String badJson = "{ \"sku\":\"MUG-LOGO-001\", \"qty\": 2 "; // missing closing brace

        mvc.perform(post(BASE_URL + "/" + CART_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:malformed-json"))
                .andExpect(jsonPath("$.title").value("Malformed JSON"));

        verifyNoInteractions(cartService);
    }

    private static CartDto emptyCart(String id, Currency currency) {
        CartDto dto = new CartDto();
        dto.setExternalId(id);
        dto.setCurrency(currency);
        dto.setItems(List.of());
        dto.setSubtotal(new BigDecimal("0.00"));
        dto.setTax(new BigDecimal("0.00"));
        dto.setTotal(new BigDecimal("0.00"));
        return dto;
    }

    private static CartDto cartWithOneItem(String id, String sku, String name, String unitPrice, int qty) {
        CartItemDto item = new CartItemDto();
        item.setProductSku(sku);
        item.setProductName(name);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(qty);
        item.setLineTotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(qty)));

        CartDto dto = new CartDto();
        dto.setExternalId(id);
        dto.setCurrency(Currency.EUR);
        dto.setItems(List.of(item));
        dto.setSubtotal(item.getLineTotal());
        dto.setTax(new BigDecimal("0.00"));
        dto.setTotal(item.getLineTotal());
        return dto;
    }

}
