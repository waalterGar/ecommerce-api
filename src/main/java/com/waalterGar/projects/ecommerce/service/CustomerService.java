package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto;

import java.util.List;

public interface CustomerService {
    List<CustomerDto> getAllCustomers();

    CustomerDto createCustomer(CreateCustomerDto customerDto);

    CustomerDto getCustomerByExternalId(String externalId);

    CustomerDto updateCustomer(String externalId, UpdateCustomerDto dto);
}
