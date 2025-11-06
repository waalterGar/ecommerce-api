package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.api.pagination.PageEnvelope;
import com.waalterGar.projects.ecommerce.api.pagination.config.CustomerSortConfig;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.CustomerService;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.utils.CountryCode;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@Import({ GlobalExceptionHandler.class, CustomerSortConfig.class })
@EnableConfigurationProperties(PaginationProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CustomerController paging (GET /customers) tests")
public class CustomerControllerPagingTest {
    private static final String BASE_URL = "/customers";

    @Autowired MockMvc mvc;

    @MockitoBean CustomerService customerService;
    @MockitoBean OrderService orderService;

    @Test
    @DisplayName("GET /customers returns default PageEnvelope")
    void list_defaultPaging() throws Exception {
        var items = List.of(customer("cust-1","Jane","Doe","jane@example.com"));
        var envelope = new PageEnvelope<>(
                items, 0, 20, 1L, 1, false, false, List.of("createdAt,desc")
        );
        when(customerService.list(isNull(), isNull(), any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"))
                .andExpect(jsonPath("$.items[0].externalId").value("cust-1"))
                .andExpect(jsonPath("$.items[0].email").value("jane@example.com"));
    }

    @Test
    @DisplayName("GET /customers supports custom page/size and single-sort")
    void list_customPagingAndSingleSort() throws Exception {
        var items = List.of(customer("cust-2","John","Smith","john@example.com"));
        var envelope = new PageEnvelope<>(
                items, 1, 5, 6L, 2, true, true, List.of("lastName,asc")
        );
        when(customerService.list(isNull(), isNull(), any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "lastName,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort[0]").value("lastName,asc"))
                .andExpect(jsonPath("$.items[0].externalId").value("cust-2"));
    }

    @Test
    @DisplayName("GET /customers accepts filters (email & q) and returns envelope")
    void list_withFilters_emailAndQ() throws Exception {
        var items = List.of(customer("cust-3","Alicia","Doe","alicia.doe@example.com"));
        var envelope = new PageEnvelope<>(
                items, 0, 20, 1L, 1, false, false, List.of("createdAt,desc")
        );
        when(customerService.list(eq("alicia.doe@example.com"), eq("doe"), any())).thenReturn(envelope);

        mvc.perform(get(BASE_URL)
                        .param("email", "alicia.doe@example.com")
                        .param("q", "doe")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("alicia.doe@example.com"));
    }

    // ---------- Validation / ProblemDetails ----------

    @Test
    @DisplayName("GET /customers with invalid size → 400 invalid-pagination")
    void list_invalidSize() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-pagination"))
                .andExpect(jsonPath("$.title").value("Invalid pagination parameters"));
    }

    @Test
    @DisplayName("GET /customers with disallowed sort field → 400 invalid-sort")
    void list_invalidSortField() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("sort", "hackerField,asc")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-sort"))
                .andExpect(jsonPath("$.title").value("Invalid sort parameter"))
                .andExpect(jsonPath("$.allowedFields").isArray());
    }

    // ---------- helpers ----------

    private static CustomerDto customer(String id, String first, String last, String email) {
        return new CustomerDto(
                id, first, last, email,
                "+34 600 000 000",
                "Addr 1", "City", "ST", "00000",
                CountryCode.ES, true
        );
    }
}

