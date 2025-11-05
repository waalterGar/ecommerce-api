package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
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
        CreateCustomerDto input = defaultDto();
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
    @DisplayName("createCustomer: null body → IllegalArgumentException")
    void createCustomer_nullBody_throws() {
        assertThatThrownBy(() -> customerService.createCustomer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Body is required");
    }

    @Test
    @DisplayName("createCustomer: duplicate email → IllegalArgumentException")
    void createCustomer_duplicateEmail_rejected() {
        CreateCustomerDto input = defaultDto();
        // Service checks uniqueness; prefer existsByEmail for speed if repo exposes it
        when(customerRepository.existsByEmail(EMAIL.toLowerCase())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
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

    // --------- updateCustomer tests ---------
    @Test
    @DisplayName("updateCustomer: updates partial fields (name/phone) and returns DTO")
    void updateCustomer_partialUpdate_success() {
        // Existing customer in repo
        Customer existing = defaultEntity();
        existing.setId(UUID.randomUUID());
        when(customerRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(existing));

        // Update firstName + phone only
        var upd = new com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto();
        upd.setFirstName("Johnny");
        upd.setPhoneNumber("+34 600 111 222");

        CustomerDto out = customerService.updateCustomer(EXT_ID, upd);

        assertThat(out.getFirstName()).isEqualTo("Johnny");
        assertThat(out.getPhoneNumber()).isEqualTo("+34 600 111 222");
        // unchanged fields remain
        assertThat(out.getEmail()).isEqualTo(EMAIL);
        verify(customerRepository).findByExternalId(EXT_ID);
    }

    @Test
    @DisplayName("updateCustomer: changing email to a unique value succeeds")
    void updateCustomer_changeEmail_unique_ok() {
        Customer existing = defaultEntity();
        existing.setId(UUID.randomUUID());
        when(customerRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(existing));

        var upd = new com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto();
        upd.setEmail("new.mail@example.com");

        // Unique: repo.findByEmail returns empty
        when(customerRepository.findByEmail("new.mail@example.com")).thenReturn(Optional.empty());

        CustomerDto out = customerService.updateCustomer(EXT_ID, upd);

        assertThat(out.getEmail()).isEqualTo("new.mail@example.com");
        verify(customerRepository).findByExternalId(EXT_ID);
        verify(customerRepository).findByEmail("new.mail@example.com");
    }

    @Test
    @DisplayName("updateCustomer: changing email to one used by another customer → IllegalArgumentException")
    void updateCustomer_changeEmail_duplicate_rejected() {
        // Current customer
        Customer target = defaultEntity();
        target.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(customerRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(target));

        // Another customer already uses the new email
        Customer other = defaultEntity();
        other.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        other.setEmail("taken@example.com");
        when(customerRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        var upd = new com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto();
        upd.setEmail("taken@example.com");

        assertThatThrownBy(() -> customerService.updateCustomer(EXT_ID, upd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(customerRepository).findByExternalId(EXT_ID);
        verify(customerRepository).findByEmail("taken@example.com");
    }

    @Test
    @DisplayName("updateCustomer: non-existent externalId → NoSuchElementException('Customer not found')")
    void updateCustomer_missingCustomer_throws() {
        when(customerRepository.findByExternalId(EXT_ID)).thenReturn(Optional.empty());

        var upd = new com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto();
        upd.setFirstName("X");

        assertThatThrownBy(() -> customerService.updateCustomer(EXT_ID, upd))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Customer not found");

        verify(customerRepository).findByExternalId(EXT_ID);
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

    private CreateCustomerDto defaultDto() {
        CreateCustomerDto dto = new CreateCustomerDto();
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setEmail(EMAIL);
        dto.setPhone("+34 600123456");
        dto.setAddress("Fake Street 123");
        dto.setCity("Chicago");
        dto.setState("IL");
        dto.setZipCode("60622");
        dto.setCountryCode(COUNTRY);
        return dto;
    }
}