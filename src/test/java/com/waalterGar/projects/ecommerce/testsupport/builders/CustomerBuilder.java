package com.waalterGar.projects.ecommerce.testsupport.builders;

import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.utils.CountryCode;

import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerBuilder {
    private String externalId = UUID.randomUUID().toString();
    private boolean isActive = true;
    private String firstName = "John";
    private String lastName = "Doe";
    private String email = "john.doe@example.com";
    private String phoneNumber = "123456789";
    private String address = "123 Main St";
    private String city = "Madrid";
    private String state = "Madrid";
    private String zipCode = "28001";
    private CountryCode countryCode = CountryCode.ES;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CustomerBuilder withExternalId(String id) { this.externalId = id; return this; }
    public CustomerBuilder inactive() { this.isActive = false; return this; }
    public CustomerBuilder withFirstName(String fn) { this.firstName = fn; return this; }
    public CustomerBuilder withLastName(String ln) { this.lastName = ln; return this; }
    public CustomerBuilder withEmail(String em) { this.email = em; return this; }
    public CustomerBuilder withPhoneNumber(String pn) { this.phoneNumber = pn; return this; }
    public CustomerBuilder withAddress(String addr) { this.address = addr; return this; }
    public CustomerBuilder withCity(String city) { this.city = city; return this; }
    public CustomerBuilder withState(String state) { this.state = state; return this; }
    public CustomerBuilder withZipCode(String zip) { this.zipCode = zip; return this; }
    public CustomerBuilder withCountryCode(CountryCode cc) { this.countryCode = cc; return this; }
    public CustomerBuilder withCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public CustomerBuilder withUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

    public Customer build() {
        Customer c = new Customer();
        c.setExternalId(externalId);
        c.setIsActive(isActive);
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setEmail(email);
        c.setPhoneNumber(phoneNumber);
        c.setAddress(address);
        c.setCity(city);
        c.setState(state);
        c.setZipCode(zipCode);
        c.setCountryCode(countryCode);
        c.setCreatedAt(createdAt);
        c.setUpdatedAt(updatedAt);
        return c;
    }
}