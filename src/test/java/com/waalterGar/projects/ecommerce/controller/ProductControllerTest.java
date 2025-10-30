package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.ActivationProductDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateProductDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.api.pagination.config.OrderSortConfig;
import com.waalterGar.projects.ecommerce.api.pagination.config.ProductSortConfig;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.ProductService;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ProductController.class)
@Import({ GlobalExceptionHandler.class, ProductSortConfig.class })
@EnableConfigurationProperties(PaginationProperties.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {
    private static final String BASE_URL = "/products";
    private static final String SKU = "MUG-LOGO-001";
    private static final String URL = BASE_URL + "/" + SKU;

    @Autowired MockMvc mvc;
    @MockitoBean ProductService productService;

    @Test
    void put_valid_returns200_withBody() throws Exception {
        String payload = """
        { "name":"Mug v2","description":"d","price":10.99,"stockQuantity":180,"isActive":true }
        """;

        ProductDto returned = new ProductDto();
        returned.setSku(SKU);
        returned.setName("Mug v2");
        returned.setDescription("d");
        returned.setPrice(new BigDecimal("10.99"));
        returned.setCurrency(Currency.EUR);
        returned.setStockQuantity(180);
        returned.setIsActive(true);

        when(productService.updateProduct(eq(SKU), any(UpdateProductDto.class))).thenReturn(returned);

        mvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sku").value(SKU))
                .andExpect(jsonPath("$.name").value("Mug v2"))
                .andExpect(handler().handlerType(ProductController.class))
                .andExpect(handler().methodName("updateProduct"));

        verify(productService).updateProduct(eq(SKU), any(UpdateProductDto.class));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void put_invalidPayload_returns400_problem() throws Exception {
        // blank name, negative numbers
        String bad = """
        { "name":"", "description":"d", "price":-1, "stockQuantity":-5, "isActive":true }
        """;

        mvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"))
                .andExpect(jsonPath("$.title").value("Validation Failed"));

        verifyNoInteractions(productService);
    }

    @Test
    void put_notFound_returns404_problem() throws Exception {
        String payload = """
        { "name":"Mug v2","description":"d","price":10.99,"stockQuantity":180,"isActive":true }
        """;

        when(productService.updateProduct(eq(SKU), any(UpdateProductDto.class)))
                .thenThrow(new NoSuchElementException("Product not found: " + SKU));

        mvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        verify(productService).updateProduct(eq(SKU), any(UpdateProductDto.class));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void put_conflict_returns409_problem() throws Exception {
        String payload = """
        { "name":"Mug v2","description":"d","price":10.99,"stockQuantity":180,"isActive":true }
        """;

        when(productService.updateProduct(eq(SKU), any(UpdateProductDto.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

        mvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:conflict"))
                .andExpect(jsonPath("$.title").value("Optimistic Lock Conflict"));

        verify(productService).updateProduct(eq(SKU), any(UpdateProductDto.class));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void put_unsupportedMediaType_returns415_problem() throws Exception {
        mvc.perform(put(URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Mug v2"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:unsupported-media-type"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));

        verifyNoInteractions(productService);
    }

    @Test
    void put_malformedJson_returns400_problem() throws Exception {
        String badJson = "{ \"name\":\"Mug v2\", \"price\": 10.99 "; // missing closing brace

        mvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:malformed-json"))
                .andExpect(jsonPath("$.title").value("Malformed JSON"));

        verifyNoInteractions(productService);
    }

    @Test
    void patch_activation_valid_returns200() throws Exception {
        String sku = "MUG-LOGO-001";
        String url = "/products/" + sku + "/activation";
        String payload = "{\"isActive\": true}";

        ProductDto returned = new ProductDto();
        returned.setSku(sku);
        returned.setName("Mug");
        returned.setDescription("d");
        returned.setPrice(new BigDecimal("10.99"));
        returned.setCurrency(Currency.EUR);
        returned.setStockQuantity(100);
        returned.setIsActive(true);

        when(productService.setProductActive(eq(sku), any(ActivationProductDto.class))).thenReturn(returned);

        mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(handler().methodName("setActivation"));

        verify(productService).setProductActive(eq(sku), any(ActivationProductDto.class));
        verifyNoMoreInteractions(productService);
    }

    @Test
    void patch_activation_invalidPayload_returns400_problem() throws Exception {
        String sku = "MUG-LOGO-001";
        String url = "/products/" + sku + "/activation";
        // missing isActive
        mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"));
        verifyNoInteractions(productService);
    }
}
