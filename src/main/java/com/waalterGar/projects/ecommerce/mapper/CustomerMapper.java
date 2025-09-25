package com.waalterGar.projects.ecommerce.mapper;

import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.entity.Customer;

public class CustomerMapper {
    public static CustomerDto toDto(Customer customer){
        return new CustomerDto(
                customer.getExternalId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getZipCode(),
                customer.getCountryCode(),
                customer.getIsActive()
        );
    }

    public static Customer toEntity(CustomerDto customerDto){
        Customer customer = new Customer();
        customer.setExternalId(customerDto.getExternalId());
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setEmail(customerDto.getEmail());
        customer.setPhoneNumber(customerDto.getPhoneNumber());
        customer.setAddress(customerDto.getAddress());
        customer.setCity(customerDto.getCity());
        customer.setState(customerDto.getState());
        customer.setZipCode(customerDto.getZipCode());
        customer.setCountryCode(customerDto.getCountryCode());
        customer.setIsActive(customerDto.getIsActive());
        return customer;
    }
}
