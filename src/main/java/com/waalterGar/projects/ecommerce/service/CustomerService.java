package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;

import java.util.List;

public interface CustomerService {
    List<CustomerDto> getAllCustomers();

    CustomerDto createCustomer(CustomerDto customerDto);

    CustomerDto getCustomerByExternalId(String externalId);
}
