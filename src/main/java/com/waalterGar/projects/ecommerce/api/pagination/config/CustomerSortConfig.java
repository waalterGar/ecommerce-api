package com.waalterGar.projects.ecommerce.api.pagination.config;

import com.waalterGar.projects.ecommerce.api.pagination.AllowedSorts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class CustomerSortConfig {

    @Bean
    public AllowedSorts customersAllowedSorts() {
        return new AllowedSorts(Set.of("createdAt", "lastName", "email"));
    }
}
