package com.waalterGar.projects.ecommerce.api.pagination.config;

import com.waalterGar.projects.ecommerce.api.pagination.AllowedSorts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class OrderSortConfig {

    @Bean
    public AllowedSorts ordersAllowedSorts() {
        return new AllowedSorts(Set.of(
                "id",
                "createdAt",
                "updatedAt",
                "status",
                "totalAmount"
        ));
    }
}
