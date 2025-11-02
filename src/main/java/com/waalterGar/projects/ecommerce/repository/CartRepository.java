package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID>{
    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByExternalId(String externalId);
}
