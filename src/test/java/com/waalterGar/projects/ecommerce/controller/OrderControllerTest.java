package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.service.OrderService;
import jakarta.validation.UnexpectedTypeException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {
    private static final String BASE_URL = "/orders";
    private static final String UNKNOWN_ORDER_ID = "ord-does-not-exist";
    private static final String ORDER_ID = "ord-123";
    private static final String UNKNOWN_URL = BASE_URL + "/" + UNKNOWN_ORDER_ID;
    private static final String ORDER_URL = BASE_URL + "/" + ORDER_ID;

    @Autowired MockMvc mvc;
    @MockitoBean OrderService orderService;

    @Test
    void getByExternalId_found_returns200() throws Exception {
        OrderDto dto = new OrderDto();
        dto.setExternalId(ORDER_ID);
        when(orderService.getOrderByExternalId(ORDER_ID)).thenReturn(dto);

        mvc.perform(get(ORDER_URL))  // full path including class-level prefix
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(handler().handlerType(OrderController.class))
                .andExpect(handler().methodName("getOrderByOrderNumber"))
                .andExpect(jsonPath("$.externalId").value(ORDER_ID));

        verify(orderService).getOrderByExternalId(ORDER_ID);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getByExternalId_missing_returns404_problem() throws Exception {

        when(orderService.getOrderByExternalId(UNKNOWN_ORDER_ID))
                .thenThrow(new NoSuchElementException("Order not found"));

        mvc.perform(get(UNKNOWN_URL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(UNKNOWN_URL))
                .andExpect(handler().handlerType(OrderController.class));

        verify(orderService).getOrderByExternalId(UNKNOWN_ORDER_ID);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getByExternalId_badInput_returns400_problem() throws Exception {
        // Arrange
        String bad = "bad";
        when(orderService.getOrderByExternalId(bad))
                .thenThrow(new IllegalArgumentException("Invalid externalId"));

        // Act + Assert
        mvc.perform(get(BASE_URL + "/" + bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Invalid externalId"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(handler().handlerType(OrderController.class));

        verify(orderService).getOrderByExternalId(bad);
        verifyNoMoreInteractions(orderService);
    }


   @Test
    void createOrder_invalidPayload_returns400_problem() throws Exception {
       doAnswer(inv -> { throw new AssertionError("Service should not be called for invalid payload"); })
               .when(orderService).createOrder(any());

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void createOrder_unexpectedTypeException_returns400_problem() throws Exception {
        // Arrange: valid payload so it doesn't fail earlier
        String validPayload = """
      {
        "customerExternalId": "cust-123",
        "currency": "USD",
        "items": [ { "productSku": "SKU-1", "quantity": 1 } ]
      }""";

        when(orderService.createOrder(any()))
                .thenThrow(new UnexpectedTypeException("HV000030: No validator for ..."));

        // Act + Assert
        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"))
                .andExpect(jsonPath("$.title").value("Invalid Constraint Configuration"))
                .andExpect(handler().handlerType(OrderController.class));

        verify(orderService).createOrder(any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_malformedJson_returns400_problem() throws Exception {
        String badJson = "{ \"customerExternalId\": \"cust-123\", \"items\": [ { \"productSku\": \"SKU-1\", \"quantity\": 1 } ]"; // missing closing }

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:malformed-json"))
                .andExpect(jsonPath("$.title").value("Malformed JSON"))
                .andExpect(handler().handlerType(OrderController.class)); // mapping matched OrderController before read failed

        verifyNoInteractions(orderService);
    }

    @Test
    void unknownRoute_returns404_problem() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:no-resource"))
                .andExpect(jsonPath("$.title").value("No Resource Found"));

        verifyNoInteractions(orderService);
    }
}