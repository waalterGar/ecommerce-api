package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionReference(String transactionReference);
    Optional<Payment> findByOrder_ExternalIdAndTransactionReference(String externalId, String transactionReference);
}