package com.waalterGar.projects.ecommerce.controller;


import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto;
import com.waalterGar.projects.ecommerce.service.CustomerService;
import com.waalterGar.projects.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/customers")
@RestController
public class CustomerController {
    private final CustomerService customerService;

    @Operation(summary = "Create customer")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE }
    )
    public ResponseEntity<CustomerDto> createCustomer(
            @Valid @RequestBody CreateCustomerDto body
    ) {
        CustomerDto created = customerService.createCustomer(body);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Update customer (partial)")
    @PatchMapping(
            path = "/{externalId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE }
    )
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable String externalId,
            @Valid @RequestBody UpdateCustomerDto body
    ) {
        CustomerDto updated = customerService.updateCustomer(externalId, body);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<CustomerDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<CustomerDto> getCustomerByExternalId(@PathVariable String externalId) {
        CustomerDto customer = customerService.getCustomerByExternalId(externalId);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }
}
