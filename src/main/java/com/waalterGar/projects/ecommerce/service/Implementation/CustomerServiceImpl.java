package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.CreateCustomerDto;
import com.waalterGar.projects.ecommerce.Dto.CustomerDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCustomerDto;
import com.waalterGar.projects.ecommerce.api.pagination.PageEnvelope;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.mapper.CustomerMapper;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    @Override
    public List<CustomerDto> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream().map(customer -> CustomerMapper.toDto(customer)).collect(Collectors.toList());
    }

    @Override
    public CustomerDto getCustomerByExternalId(String externalId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
        return CustomerMapper.toDto(customer);
    }

    @Override
    public CustomerDto createCustomer(CreateCustomerDto createCustomerDto) {
        if (createCustomerDto == null) throw new IllegalArgumentException("Body is required");
        if (customerRepository.existsByEmail(createCustomerDto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Customer customer = CustomerMapper.toEntity(createCustomerDto);
        customer.setExternalId(UUID.randomUUID().toString());
        Customer savedCustomer = customerRepository.save(customer);
        return CustomerMapper.toDto(savedCustomer);
    }

    @Transactional
    @Override
    public CustomerDto updateCustomer(String externalId, UpdateCustomerDto dto) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Invalid externalId");
        }
        Customer c = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));

        //check if email is changing
        if (dto != null && dto.getEmail() != null) {
            String newEmail = dto.getEmail().trim();
            customerRepository.findByEmail(newEmail).ifPresent(existing -> {
                if (!existing.getId().equals(c.getId())) {
                    throw new IllegalArgumentException("Email already in use");
                }
            });
        }

        CustomerMapper.applyUpdate(c, dto);
        return CustomerMapper.toDto(c);
    }

    @Override
    public PageEnvelope<CustomerDto> list(String email, String q, Pageable pageable) {
        Page<Customer> page;
        if (email != null && !email.isBlank()) {
            page = customerRepository.findByEmailContainingIgnoreCase(email.trim(), pageable);
        } else if (q != null && !q.isBlank()) {
            String like = q.trim();
            page = customerRepository.findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
                    like, like, like, pageable);
        } else {
            page = customerRepository.findAll(pageable);
        }
        return PageEnvelope.of(page.map(CustomerMapper::toDto));
    }
}
