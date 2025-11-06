package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.CountryCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCustomerDto {
    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Email
    @Size(max = 180)
    private String email;

    @Size(max = 30)
    private String phoneNumber;

    @Size(max = 120)
    private String address;

    @Size(max = 80)
    private String city;

    @Size(max = 80)
    private String state;

    @Size(max = 20)
    private String zipCode;

    private CountryCode countryCode;

    private Boolean isActive;
}
