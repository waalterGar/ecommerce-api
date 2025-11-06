package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByExternalId(String externalId);
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<Customer>
    findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
            String emailLike, String lastNameLike, String firstNameLike, Pageable pageable);

    Page<Customer>
    findByEmailContainingIgnoreCase(String emailLike, Pageable pageable);
}
