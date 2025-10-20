package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
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

    @Autowired
    MockMvc mvc;
    @MockitoBean
    OrderService orderService;

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
        doAnswer(inv -> {
            throw new AssertionError("Service should not be called for invalid payload");
        })
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

    @Test
    void createOrder_valid_returns201_withBody() throws Exception {
        String payload = """
                {
                  "customerExternalId": "cust-123",
                  "currency": "EUR",
                  "items": [ { "productSku": "SKU-1", "quantity": 2 } ]
                }""";

        OrderDto returned = new OrderDto();
        returned.setExternalId("ord-xyz");

        when(orderService.createOrder(any())).thenReturn(returned);

        mvc.perform(post(BASE_URL) // BASE_URL = "/api/orders"
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value("ord-xyz"))
                .andExpect(handler().handlerType(OrderController.class))
                .andExpect(handler().methodName("createOrder"));

        verify(orderService).createOrder(any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_unsupportedMediaType_returns415_problem() throws Exception {
        String badPayload = "this is not json";

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.TEXT_PLAIN) // wrong content type
                        .content(badPayload))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:unsupported-media-type"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_notAcceptable_returns406_problem() throws Exception {
        String payload = """
                  {
                    "customerExternalId": "cust-123",
                    "currency": "EUR",
                    "items": [ { "productSku": "SKU-1", "quantity": 1 } ]
                  }
                """;

        OrderDto dto = new OrderDto();
        dto.setExternalId("ord-abc");
        when(orderService.createOrder(any())).thenReturn(dto);

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-acceptable"))
                .andExpect(jsonPath("$.title").value("Not Acceptable"));
    }

    @Test
    void createOrder_insufficientStock_returns422_problem() throws Exception {
        String payload = """
                {
                  "customerExternalId": "cust-123",
                   "currency": "EUR",
                  "items": [ { "productSku": "SKU-1", "quantity": 999 } ]
                }
                """;
        when(orderService.createOrder(any()))
                .thenThrow(new com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException(
                        "Insufficient stock for SKU SKU-1"));

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_PLAIN)
                        .content(payload))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:insufficient-stock"))
                .andExpect(jsonPath("$.title").value("Insufficient Stock"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(handler().handlerType(OrderController.class))
                .andExpect(handler().methodName("createOrder"));
    }

    @Test
    void createOrder_optimisticLockConflict_returns409_problem() throws Exception {
        String payload = """
    {
      "customerExternalId": "cust-123",
      "currency": "EUR",
      "items": [ { "productSku": "SKU-1", "quantity": 1 } ]
    }""";

        when(orderService.createOrder(any()))
                .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Concurrent update conflict"));

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:conflict"))
                .andExpect(jsonPath("$.title").value("Optimistic Lock Conflict"))
                .andExpect(handler().handlerType(OrderController.class))
                .andExpect(handler().methodName("createOrder"));

        verify(orderService).createOrder(any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_inactiveProduct_returns422_problem() throws Exception {
        String payload = """
    {
      "customerExternalId": "cust-123",
      "currency": "EUR",
      "items": [ { "productSku": "SKU-1", "quantity": 1 } ]
    }""";

        when(orderService.createOrder(any()))
                .thenThrow(new InactiveProductException("Product is inactive: SKU-1"));

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:inactive-product"))
                .andExpect(jsonPath("$.title").value("Inactive Product"))
                .andExpect(jsonPath("$.detail").value("Product is inactive: SKU-1"));

        verify(orderService).createOrder(any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void pay_happy_returns200() throws Exception {
        String url = "/orders/ord-123/pay";
        String payload = """
      { "amount": 39.98, "currency": "EUR", "provider": "stripe", "transactionReference": "tx-1" }
    """;

        OrderDto returned = new OrderDto();
        returned.setExternalId("ord-123");
        // set other fields as needed…

        when(orderService.pay(eq("ord-123"), any())).thenReturn(returned);

        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value("ord-123"))
                .andExpect(handler().methodName("pay"));

        verify(orderService).pay(eq("ord-123"), any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void pay_invalidState_returns400_problem() throws Exception {
        String url = "/orders/ord-123/pay";
        when(orderService.pay(eq("ord-123"), any()))
                .thenThrow(new IllegalArgumentException("Order not payable from status CANCELED"));

        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Order not payable from status CANCELED"));

        verify(orderService).pay(eq("ord-123"), any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void pay_amountMismatch_returns400_problem() throws Exception {
        String url = "/orders/ord-123/pay";
        when(orderService.pay(eq("ord-123"), any()))
                .thenThrow(new IllegalArgumentException("Amount mismatch"));

        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        { "amount": 39.98 }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Amount mismatch"));

        verify(orderService).pay(eq("ord-123"), any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void pay_notFound_returns404_problem() throws Exception {
        String url = "/orders/ord-404/pay";
        when(orderService.pay(eq("ord-404"), any()))
                .thenThrow(new NoSuchElementException("Order not found"));

        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Order not found"));

        verify(orderService).pay(eq("ord-404"), any());
        verifyNoMoreInteractions(orderService);
    }
}