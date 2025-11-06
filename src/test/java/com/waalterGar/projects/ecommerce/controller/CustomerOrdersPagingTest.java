package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.api.pagination.PageEnvelope;
import com.waalterGar.projects.ecommerce.api.pagination.config.CustomerSortConfig;
import com.waalterGar.projects.ecommerce.api.pagination.config.OrderSortConfig;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.CustomerService;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@Import({ GlobalExceptionHandler.class, OrderSortConfig.class, CustomerSortConfig.class })
@EnableConfigurationProperties(PaginationProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CustomerOrdersPagingTest {

    private static final String BASE = "/customers";
    private static final String CUST = "cust-123";

    @Autowired MockMvc mvc;

    @MockitoBean OrderService orderService;
    @MockitoBean CustomerService customerService;

    @Test
    @DisplayName("GET /customers/{id}/orders returns PageEnvelope (default paging)")
    void listOrders_defaultPaging() throws Exception {
        List<OrderDto> items = List.of(order("ord-1", new BigDecimal("19.99"), Currency.EUR, OrderStatus.CREATED));
        PageEnvelope<OrderDto> env = new PageEnvelope<>(items, 0, 20, 1L, 1, false, false, List.of("createdAt,desc"));

        when(orderService.listByCustomer(eq(CUST), any())).thenReturn(env);

        mvc.perform(get(BASE + "/" + CUST + "/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].externalId").value("ord-1"))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
    }

    @Test
    @DisplayName("GET /customers/{id}/orders supports custom page/size and single-sort")
    void listOrders_customPagingAndSort() throws Exception {
        List<OrderDto> items = List.of(order("ord-2", new BigDecimal("35.00"), Currency.EUR, OrderStatus.PAID));
        PageEnvelope<OrderDto> env = new PageEnvelope<>(items, 1, 5, 6L, 2, true, true, List.of("createdAt,desc"));

        when(orderService.listByCustomer(eq(CUST), any())).thenReturn(env);

        mvc.perform(get(BASE + "/" + CUST + "/orders")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "createdAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"))
                .andExpect(jsonPath("$.items[0].externalId").value("ord-2"));
    }

    @Test
    @DisplayName("GET /customers/{id}/orders returns 200 with empty items when no orders")
    void listOrders_empty_returnsEmptyEnvelope() throws Exception {
        PageEnvelope<OrderDto> env = new PageEnvelope<>(
                List.of(), 0, 20, 0L, 0, true, true, List.of("createdAt,desc")
        );

        when(orderService.listByCustomer(eq(CUST), any())).thenReturn(env);

        mvc.perform(get(BASE + "/" + CUST + "/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
    }

    @Test
    @DisplayName("GET /customers/{id}/orders invalid size → 400 invalid-pagination")
    void listOrders_invalidSize() throws Exception {
        mvc.perform(get(BASE + "/" + CUST + "/orders")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-pagination"))
                .andExpect(jsonPath("$.title").value("Invalid pagination parameters"));
    }

    @Test
    @DisplayName("GET /customers/{id}/orders invalid sort → 400 invalid-sort")
    void listOrders_invalidSort() throws Exception {
        mvc.perform(get(BASE + "/" + CUST + "/orders")
                        .param("sort", "hackerField,asc")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-sort"))
                .andExpect(jsonPath("$.title").value("Invalid sort parameter"))
                .andExpect(jsonPath("$.allowedFields").isArray());
    }

    @Test
    @DisplayName("GET /customers/{id}/orders customer not found → 404 problem")
    void listOrders_customerNotFound() throws Exception {
        when(orderService.listByCustomer(eq(CUST), any()))
                .thenThrow(new NoSuchElementException("Customer not found"));

        mvc.perform(get(BASE + "/" + CUST + "/orders").accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Customer not found"));
    }

    private static OrderDto order(String extId, BigDecimal total, Currency ccy, OrderStatus st) {
        OrderDto dto = new OrderDto();
        dto.setExternalId(extId);
        dto.setTotalAmount(total);
        dto.setCurrency(ccy);
        dto.setStatus(st);
        return dto;
    }
}