package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository  extends JpaRepository <Order, UUID> {
    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"items", "customer"})
    @Query("select o from Order o")
    List<Order> findAllWithItemsAndCustomer();
}
