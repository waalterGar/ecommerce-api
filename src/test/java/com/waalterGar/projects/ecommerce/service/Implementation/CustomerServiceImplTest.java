package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.utils.CountryCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl tests")
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    // Default test values
    private static final String EXT_ID = "b6a3c8d1-1234-4a56-8e90-abcdef012345";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME   = "Doe";
    private static final String EMAIL  = "john.doe@example.com";
    private static final CountryCode COUNTRY = CountryCode.US;

    @Test
    @DisplayName("createCustomer: should save and return CustomerDto")
    void createCustomer_withValidDto_returnsCustomerDto() {
        // Given
        CustomerDto input = defaultDto();
        Customer saved = defaultEntity();
        saved.setId(UUID.randomUUID());          // simulate DB-generated ID
        saved.setEmail(EMAIL.toLowerCase());
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        // When
        CustomerDto result = customerService.createCustomer(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(result.getLastName()).isEqualTo(LAST_NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL.toLowerCase());
        assertThat(result.getExternalId()).isEqualTo(EXT_ID);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("getCustomerByExternalId: should return CustomerDto when found")
    void getCustomerByExternalId_withExistingExternalId_returnsCustomerDto() {
        // Given
        Customer entity = defaultEntity();
        entity.setId(UUID.randomUUID());
        when(customerRepository.findByExternalId(eq(EXT_ID))).thenReturn(Optional.of(entity));

        // When
        CustomerDto result = customerService.getCustomerByExternalId(EXT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isEqualTo(EXT_ID);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        verify(customerRepository).findByExternalId(eq(EXT_ID));
    }

    @Test
    @DisplayName("getCustomerByExternalId: should throw NoSuchElementException when not found")
    void getCustomerByExternalId_withNonExistentExternalId_throwsNoSuchElementException() {
        // Given
        String missing = "00000000-0000-0000-0000-000000000000";
        when(customerRepository.findByExternalId(eq(missing))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> customerService.getCustomerByExternalId(missing))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Customer not found");

        verify(customerRepository).findByExternalId(eq(missing));
    }

    @Test
    @DisplayName("getAllCustomers: should return empty list when no customers exist")
    void getAllCustomers_withNoCustomers_returnsEmptyList() {
        // Given
        when(customerRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<CustomerDto> result = customerService.getAllCustomers();

        // Then
        assertThat(result).isNotNull().isEmpty();
        verify(customerRepository).findAll();
    }

    // --------- Helper methods ---------

    private Customer defaultEntity() {
        Customer c = new Customer();
        c.setExternalId(EXT_ID);
        c.setFirstName(FIRST_NAME);
        c.setLastName(LAST_NAME);
        c.setEmail(EMAIL);
        c.setPhoneNumber("+34 600123456");
        c.setAddress("Fake Street 123");
        c.setCity("Chicago");
        c.setState("IL");
        c.setZipCode("60622");
        c.setCountryCode(COUNTRY);
        c.setIsActive(true);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    private CustomerDto defaultDto() {
        CustomerDto dto = new CustomerDto();
        dto.setExternalId(EXT_ID);
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setEmail(EMAIL);
        dto.setPhoneNumber("+34 600123456");
        dto.setAddress("Fake Street 123");
        dto.setCity("Chicago");
        dto.setState("IL");
        dto.setZipCode("60622");
        dto.setCountryCode(COUNTRY);
        dto.setIsActive(true);
        return dto;
    }
}