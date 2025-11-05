package com.waalterGar.projects.ecommerce.mapper;

import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto;
import com.waalterGar.projects.ecommerce.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {}

    // ---------- Read model ----------
    public static CustomerDto toDto(Customer c) {
        if (c == null) return null;
        return new CustomerDto(
                c.getExternalId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhoneNumber(),
                c.getAddress(),
                c.getCity(),
                c.getState(),
                c.getZipCode(),
                c.getCountryCode(),
                c.getIsActive()
        );
    }

    public static Customer toEntity(CreateCustomerDto dto) {
        if (dto == null) return null;
        Customer c = new Customer();
        // externalId is assigned in the service
        c.setFirstName(normalizeName(dto.getFirstName()));
        c.setLastName(normalizeName(dto.getLastName()));
        c.setEmail(normalizeEmail(dto.getEmail()));

        c.setPhoneNumber(trim(dto.getPhone()));
        c.setAddress(trim(dto.getAddress()));
        c.setCity(trim(dto.getCity()));
        c.setState(trim(dto.getState()));
        c.setZipCode(trim(dto.getZipCode()));
        c.setCountryCode(dto.getCountryCode());

        // default active if unspecified in create
        c.setIsActive(Boolean.TRUE);
        return c;
    }

    // ---------- Update path (partial) ----------
    public static void applyUpdate(Customer c, UpdateCustomerDto dto) {
        if (c == null || dto == null) return;

        if (dto.getEmail() != null)       c.setEmail(normalizeEmail(dto.getEmail()));
        if (dto.getFirstName() != null)   c.setFirstName(normalizeName(dto.getFirstName()));
        if (dto.getLastName() != null)    c.setLastName(normalizeName(dto.getLastName()));
        if (dto.getPhoneNumber() != null)       c.setPhoneNumber(trim(dto.getPhoneNumber()));
        if (dto.getAddress() != null)     c.setAddress(trim(dto.getAddress()));
        if (dto.getCity() != null)        c.setCity(trim(dto.getCity()));
        if (dto.getState() != null)       c.setState(trim(dto.getState()));
        if (dto.getZipCode() != null)     c.setZipCode(trim(dto.getZipCode()));
        if (dto.getCountryCode() != null) c.setCountryCode(dto.getCountryCode());
        if (dto.getIsActive() != null)    c.setIsActive(dto.getIsActive());
    }

    // ---------- Helpers ----------
    private static String trim(String v) {
        return v != null ? v.trim() : null;
    }

    private static String normalizeEmail(String v) {
        return v != null ? v.trim().toLowerCase() : null;
    }

    private static String normalizeName(String v) {
        if (v == null) return null;
        v = v.trim();
        if (v.isEmpty()) return v;
        // simple title-case first letter
        return v.substring(0, 1).toUpperCase() + v.substring(1);
    }
}
