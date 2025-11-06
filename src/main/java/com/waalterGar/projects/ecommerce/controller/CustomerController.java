package com.waalterGar.projects.ecommerce.controller;


import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto;
import com.waalterGar.projects.ecommerce.api.pagination.*;
import com.waalterGar.projects.ecommerce.api.problem.InvalidPaginationException;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.CustomerService;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping("/customers")
@RestController
public class CustomerController {
    private final CustomerService customerService;
    private final AllowedSorts customersAllowedSorts;
    private final PaginationProperties props;

    public CustomerController(CustomerService service,
                           @Qualifier("customersAllowedSorts") AllowedSorts customersAllowedSorts,
                           PaginationProperties props) {
        this.customerService = service;
        this.customersAllowedSorts = customersAllowedSorts;
        this.props = props;
    }

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

    @GetMapping("/all")
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<CustomerDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<CustomerDto> getCustomerByExternalId(@PathVariable String externalId) {
        CustomerDto customer = customerService.getCustomerByExternalId(externalId);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }

    @Operation(summary = "List customers (paged)")
    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE })
    public ResponseEntity<PageEnvelope<CustomerDto>> listCustomers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String q,
            @RequestParam MultiValueMap<String, String> query
    ) {
        int effectiveSize = (size == null) ? props.defaultSize() : size;
        if (effectiveSize < 1 || effectiveSize > props.maxSize()) {
            throw new InvalidPaginationException("size must be between 1 and " + props.maxSize());
        }

        // Preserve client-provided tokens exactly as sent: ?sort=lastName,asc&sort=createdAt,desc
        List<String> sortRaw = query.get("sort");
        List<SortDirective> directives = SortParser.parse(sortRaw);
        SortValidator.ensureAllowed(directives, customersAllowedSorts);

        Pageable pageable = PageableFactory.from(
                page,
                effectiveSize,
                directives,
                customersAllowedSorts,
                props.defaults().customers(), // <-- define default sort for customers in your PaginationProperties
                props
        );

        return ResponseEntity.ok(customerService.list(email, q, pageable));
    }

}
