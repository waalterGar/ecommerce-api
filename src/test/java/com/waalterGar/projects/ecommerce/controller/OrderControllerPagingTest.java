package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.OrderDto; // adjust if your DTO is elsewhere
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.api.pagination.config.OrderSortConfig;
import com.waalterGar.projects.ecommerce.api.pagination.PageEnvelope;
import com.waalterGar.projects.ecommerce.api.pagination.config.ProductSortConfig;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = OrderController.class)
@Import({ GlobalExceptionHandler.class, OrderSortConfig.class })
@EnableConfigurationProperties(PaginationProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class OrderControllerPagingTest {
    private static final String BASE_URL = "/orders";

    @Resource
    private MockMvc mvc;

    @MockitoBean
    private OrderService orderService;

    private static OrderDto orderDto(String id) {
        OrderDto dto = new OrderDto();
        return dto;
    }

    @Test
    @DisplayName("GET /orders returns default page envelope")
    void list_defaultPaging() throws Exception {
        var items = List.of(orderDto("O-1"));
        var envelope = new PageEnvelope<>(
                items, 0, 20, 1L, 1, false, false, List.of("createdAt,desc")
        );
        when(orderService.list(any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("GET /orders supports custom page/size and single-sort")
    void list_customPagingAndSingleSort() throws Exception {
        var items = List.of(orderDto("O-2"));
        var envelope = new PageEnvelope<>(
                items, 1, 5, 6L, 2, true, true, List.of("createdAt,desc")
        );
        when(orderService.list(any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "createdAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("GET /orders supports multi-sort")
    void list_multiSort() throws Exception {
        var items = List.of(orderDto("O-3"));
        var envelope = new PageEnvelope<>(
                items, 0, 20, 10L, 1, false, false, List.of("status,asc","createdAt,desc")
        );
        when(orderService.list(any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL)
                        .param("sort", "status,asc")
                        .param("sort", "createdAt,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort[0]").value("status,asc"))
                .andExpect(jsonPath("$.sort[1]").value("createdAt,desc"));
    }

    @Test
    @DisplayName("GET /orders returns 400 ProblemDetail for invalid size")
    void list_invalidSize() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-pagination"))
                .andExpect(jsonPath("$.title").value("Invalid pagination parameters"));
    }

    @Test
    @DisplayName("GET /orders returns 400 ProblemDetail for invalid sort field")
    void list_invalidSortField() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("sort", "hackerField,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-sort"))
                .andExpect(jsonPath("$.title").value("Invalid sort parameter"))
                .andExpect(jsonPath("$.allowedFields").isArray());
    }
}
