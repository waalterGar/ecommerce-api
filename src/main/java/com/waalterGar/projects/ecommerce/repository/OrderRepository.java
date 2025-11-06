package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository  extends JpaRepository <Order, UUID> {
    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"items", "customer"})
    @Query("select o from Order o")
    List<Order> findAllWithItemsAndCustomer();

    @EntityGraph(attributePaths = { "items", "customer" })
    Page<Order>
    findByCustomer_ExternalId(String customerExternalId, Pageable pageable);
}
