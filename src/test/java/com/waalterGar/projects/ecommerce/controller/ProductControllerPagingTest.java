package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.api.pagination.PageEnvelope;
import com.waalterGar.projects.ecommerce.api.pagination.config.ProductSortConfig;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class)
@Import({ GlobalExceptionHandler.class, ProductSortConfig.class, ProductControllerPagingTest.TestProps.class })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductControllerPagingTest {

    private static final String BASE_URL = "/products";

    @Resource
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    // Provide PaginationProperties bean for the controller under WebMvcTest
    @TestConfiguration
    static class TestProps {
        @Bean
        PaginationProperties paginationProperties() {
            return new PaginationProperties(
                    20, // defaultSize
                    100, // maxSize
                    new PaginationProperties.Defaults(
                            "createdAt,desc", // products default sort
                            "createdAt,desc",
                            "createdAt,desc"
                    )
            );
        }
    }

    private static ProductDto product(String sku, String name, String price) {
        ProductDto dto = new ProductDto();
        dto.setSku(sku);
        dto.setName(name);
        dto.setDescription("d");
        dto.setPrice(new BigDecimal(price));
        dto.setStockQuantity(5);
        dto.setIsActive(true);
        return dto;
    }

    @Test
    @DisplayName("GET /products returns default page envelope")
    void list_defaultPaging() throws Exception {
        var items = List.of(product("SKU-1","Alpha","9.99"));
        var envelope = new PageEnvelope<>(
                items, 0, 20, 1L, 1, false, false, List.of("createdAt,desc")
        );
        when(productService.list(any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
    }
    // test with 1 sort field
    @Test
    @DisplayName("GET /products supports custom page/size and single-sort")
    void list_customPagingAndSingleSort() throws Exception {
        var items = List.of(product("SKU-2","Beta","19.99"));
        var envelope = new PageEnvelope<>(
                items, 1, 5, 6L, 2, true, true, List.of("name,asc")
        );
        when(productService.list(any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,asc")

                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort[0]").value("name,asc"))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-2"));
    }

    @Test
    @DisplayName("GET /products supports custom page/size and multi-sort")
    void list_customPagingAndSort() throws Exception {
        var items = List.of(product("SKU-2","Beta","19.99"));
        var envelope = new PageEnvelope<>(
                items, 1, 5, 6L, 2, true, true, List.of("name,asc","price,desc")
        );
        when(productService.list(any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,asc")
                        .param("sort", "price,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort[0]").value("name,asc"))
                .andExpect(jsonPath("$.sort[1]").value("price,desc"))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-2"));
    }

    @Test
    @DisplayName("GET /products returns 400 ProblemDetail for invalid size")
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
    @DisplayName("GET /products returns 400 ProblemDetail for invalid sort field")
    void list_invalidSortField() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("sort", " weirdField,asc")
                        .param("sort", "price,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-sort"))
                .andExpect(jsonPath("$.title").value("Invalid sort parameter"))
                .andExpect(jsonPath("$.allowedFields").isArray());
    }
}
