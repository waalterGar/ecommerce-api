package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.CountryCode;
import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CustomerDto {
    private String externalId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private CountryCode countryCode;
    private Boolean isActive;

    public CustomerDto(String externalId, String firstName, String lastName, String email, String phoneNumber, String address, String city, String state, String zipCode, CountryCode countryCode, Boolean isActive) {
        this.externalId = externalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.countryCode = countryCode;
        this.isActive = isActive;
    }
}
