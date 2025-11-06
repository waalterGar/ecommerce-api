package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto;
import com.waalterGar.projects.ecommerce.api.GlobalExceptionHandler;
import com.waalterGar.projects.ecommerce.service.CustomerService;
import com.waalterGar.projects.ecommerce.utils.CountryCode;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class CustomerControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService customerService;

    private static final String BASE = "/customers";
    private static final String EXT_ID = "b6a3c8d1-1234-4a56-8e90-abcdef012345";

    @Test
    @DisplayName("POST /customers → 201 with body")
    void createCustomer_201() throws Exception {
        String body = """
        {
          "email": "jane.doe@example.com",
          "firstName": "Jane",
          "lastName": "Doe",
          "phone": "+34 600000000",
          "address": "Fake St 1",
          "city": "Madrid",
          "state": "M",
          "zipCode": "28001",
          "countryCode": "ES"
        }
        """;

        CustomerDto returned = sampleDto(
                EXT_ID,
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "+34 600000000",
                "Fake St 1",
                "Madrid",
                "M",
                "28001",
                CountryCode.ES, true);

        when(customerService.createCustomer(any(CreateCustomerDto.class))).thenReturn(returned);

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value(EXT_ID))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                .andExpect(handler().handlerType(CustomerController.class))
                .andExpect(handler().methodName("createCustomer"));
    }

    @Test
    @DisplayName("POST /customers invalid payload → 400 validation problem")
    void createCustomer_400_validation() throws Exception {
        // missing email, blank names → Bean Validation should hit
        String bad = """
        { "firstName":"", "lastName":"" }
        """;

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /customers duplicate email → 400 invalid-request problem")
    void createCustomer_400_duplicate() throws Exception {
        String body = """
        { "email":"taken@example.com","firstName":"Jane","lastName":"Doe" }
        """;

        when(customerService.createCustomer(any(CreateCustomerDto.class)))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Email already in use"));
    }


    // ---------- GET /customers/{externalId} ----------
    @Test
    @DisplayName("GET /customers/{externalId} → 200 with body")
    void getCustomer_200() throws Exception {
        CustomerDto dto = sampleDto(EXT_ID,
                "John",
                "Doe",
                "john@example.com",
                "+34 600",
                "Addr",
                "City",
                "State",
                "00000",
                CountryCode.ES, true);

        when(customerService.getCustomerByExternalId(eq(EXT_ID))).thenReturn(dto);

        mvc.perform(get(BASE + "/" + EXT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.externalId").value(EXT_ID))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /customers/{externalId} not found → 404 problem")
    void getCustomer_404() throws Exception {
        when(customerService.getCustomerByExternalId(eq(EXT_ID)))
                .thenThrow(new NoSuchElementException("Customer not found"));

        mvc.perform(get(BASE + "/" + EXT_ID).accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Customer not found"));
    }

    // ---------- PATCH /customers/{externalId} ----------
    @Test
    @DisplayName("PATCH /customers/{externalId} → 200 with updated fields")
    void updateCustomer_200() throws Exception {
        String body = """
        { "firstName":"Johnny", "phone":"+34 600 111 222" }
        """;

        CustomerDto updated = sampleDto(
                EXT_ID, "Johnny", "Doe", "john@example.com",
                "+34 600 111 222", "Addr", "City", "State", "00000",
                CountryCode.ES, true
        );

        when(customerService.updateCustomer(eq(EXT_ID), any(UpdateCustomerDto.class))).thenReturn(updated);

        mvc.perform(patch(BASE + "/" + EXT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.phoneNumber").value("+34 600 111 222"));
    }

    @Test
    @DisplayName("PATCH /customers/{externalId} invalid body → 400 validation problem")
    void updateCustomer_400_validation() throws Exception {
        // bad email format if provided
        String bad = """
        { "email":"not-an-email" }
        """;

        mvc.perform(patch(BASE + "/" + EXT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation"))
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("PATCH /customers/{externalId} duplicate email → 400 invalid-request problem")
    void updateCustomer_400_duplicateEmail() throws Exception {
        String body = """
        { "email":"taken@example.com" }
        """;

        when(customerService.updateCustomer(eq(EXT_ID), any(UpdateCustomerDto.class)))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        mvc.perform(patch(BASE + "/" + EXT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Email already in use"));
    }

    @Test
    @DisplayName("PATCH /customers/{externalId} not found → 404 problem")
    void updateCustomer_404() throws Exception {
        when(customerService.updateCustomer(eq(EXT_ID), any(UpdateCustomerDto.class)))
                .thenThrow(new NoSuchElementException("Customer not found"));

        String body = """
        { "firstName":"X" }
        """;

        mvc.perform(patch(BASE + "/" + EXT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Customer not found"));
    }

    // ---------- GET /customers (list) ----------

    @Test
    @DisplayName("GET /customers → 200 with array of customers")
    void getAllCustomers_returnsList() throws Exception {
        CustomerDto c1 = sampleDto(
                "cust-001",
                "Jane",
                "Doe",
                "jane@example.com",
                "+34 600 000 001",
                "Calle 1",
                "Madrid",
                "M",
                "28001",
                CountryCode.ES, true);

        CustomerDto c2 = sampleDto(
                "cust-002",
                "John",
                "Smith",
                "john@example.com",
                "+34 600 000 002",
                "Calle 2",
                "Barcelona",
                "B",
                "08001",
                CountryCode.ES, true);

        when(customerService.getAllCustomers()).thenReturn(java.util.List.of(c1, c2));

        mvc.perform(get("/customers").accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].externalId").value("cust-001"))
                .andExpect(jsonPath("$[0].email").value("jane@example.com"))
                .andExpect(jsonPath("$[1].externalId").value("cust-002"))
                .andExpect(jsonPath("$[1].email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /customers → 200 with empty array when no customers")
    void getAllCustomers_emptyList() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(java.util.List.of());

        mvc.perform(get("/customers").accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------- helper -------
    private static CustomerDto sampleDto(String externalId, String firstName, String lastName, String email, String phone, String address, String city, String state, String zip, CountryCode country, Boolean isActive
    ) {
        CustomerDto dto = new CustomerDto(externalId, firstName, lastName, email, phone, address, city, state, zip, country, isActive);
        return dto;
    }
}
