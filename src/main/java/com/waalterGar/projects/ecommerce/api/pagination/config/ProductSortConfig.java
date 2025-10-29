package com.waalterGar.projects.ecommerce.api.pagination.config;

import com.waalterGar.projects.ecommerce.api.pagination.AllowedSorts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ProductSortConfig {

    @Bean
    public AllowedSorts productsAllowedSorts() {
        // Whitelist ONLY safe fields you expose to clients
        return new AllowedSorts(Set.of(
                "id", "sku", "name", "price", "createdAt", "updatedAt"
        ));
    }
}
