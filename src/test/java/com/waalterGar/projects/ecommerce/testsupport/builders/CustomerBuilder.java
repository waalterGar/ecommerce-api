package com.waalterGar.projects.ecommerce.testsupport.builders;

import com.waalterGar.projects.ecommerce.entity.Customer;

import java.util.UUID;

public class CustomerBuilder {
    private String externalId = UUID.randomUUID().toString();
    private boolean isActive = true;

    public CustomerBuilder withExternalId(String id) { this.externalId = id; return this; }
    public CustomerBuilder inactive() { this.isActive = false; return this; }

    public Customer build() {
        Customer c = new Customer();
        c.setExternalId(externalId);
        c.setIsActive(isActive);
        return c;
    }
}