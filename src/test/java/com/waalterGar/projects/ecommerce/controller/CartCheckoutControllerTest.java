package com.waalterGar.projects.ecommerce.controller;


import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.service.CartService;
import com.waalterGar.projects.ecommerce.service.CheckoutService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = CartController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class CartCheckoutControllerTest {
    private static final String BASE_URL = "/carts";
    private static final String CART_ID = "cart-123";
    private static final String CUSTOMER_ID = "a1f4e12c-8d5c-4c1b-b3e1-7e2c1d123456";
    private static final String ORDER_ID = "ord-999";
    private static final String EXTERNAL_ID = "cart-123";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartService cartService;

    @MockitoBean CheckoutService checkoutService;

    @Test
    @DisplayName("POST /carts/{id}/checkout?customerId=... -> 201 Created with Location and body")
    void checkout_happyPath_returns201_withLocation_andBody() throws Exception {
        when(checkoutService.checkout(CART_ID, CUSTOMER_ID)).thenReturn(order(ORDER_ID));

        mvc.perform(post(BASE_URL + "/" + CART_ID + "/checkout")
                        .param("customerId", CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value(ORDER_ID))
                .andExpect(handler().handlerType(CartController.class))
                .andExpect(handler().methodName("checkout"));

        verify(checkoutService).checkout(CART_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST /carts/{externalId}/checkout without customerId -> 400 ProblemDetail")
    void checkout_missingCustomer_returns400_problem() throws Exception {
        mvc.perform(post(BASE_URL + "/" + EXTERNAL_ID + "/checkout")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:missing-param"))
                .andExpect(jsonPath("$.title").value("Missing Parameter"));

        verifyNoInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST /carts/{externalId}/checkout -> 404 when cart not found")
    void checkout_cartNotFound_returns404_problem() throws Exception {
        when(checkoutService.checkout(anyString(), anyString()))
                .thenThrow(new NoSuchElementException("Cart not found"));

        mvc.perform(post(BASE_URL + "/" + EXTERNAL_ID + "/checkout")
                        .param("customerId", CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        verify(checkoutService).checkout(EXTERNAL_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST /carts/{externalId}/checkout -> 422 when inactive product")
    void checkout_inactiveProduct_returns422_problem() throws Exception {
        when(checkoutService.checkout(anyString(), anyString()))
                .thenThrow(new InactiveProductException("Product is inactive"));

        mvc.perform(post(BASE_URL + "/" + EXTERNAL_ID + "/checkout")
                        .param("customerId", CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:inactive-product"))
                .andExpect(jsonPath("$.title").value("Inactive Product"));

        verify(checkoutService).checkout(EXTERNAL_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST /carts/{externalId}/checkout returns 422 when insufficient of stock")
    void checkout_outOfStock_returns409_problem() throws Exception {
        when(checkoutService.checkout(anyString(), anyString()))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        mvc.perform(post(BASE_URL + "/" + EXTERNAL_ID + "/checkout")
                        .param("customerId", CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:insufficient-stock"))
                .andExpect(jsonPath("$.title").value("Insufficient Stock"));

        verify(checkoutService).checkout(EXTERNAL_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST /carts/{externalId}/checkout -> 400 when invalid state (already checked out)")
    void checkout_invalidState_returns400_problem() throws Exception {
        when(checkoutService.checkout(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Cart is not editable or already checked out"));

        mvc.perform(post(BASE_URL + "/" + EXTERNAL_ID + "/checkout")
                        .param("customerId", CUSTOMER_ID)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"));

        verify(checkoutService).checkout(EXTERNAL_ID, CUSTOMER_ID);
        verifyNoMoreInteractions(checkoutService);
    }

    // Helper method
    private static OrderDto order(String externalId) {
        OrderDto dto = new OrderDto();
        dto.setExternalId(externalId);
        return dto;
    }

}
