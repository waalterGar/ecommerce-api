package com.waalterGar.projects.ecommerce;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiDocsConfig {

    @Bean
    public OpenAPI ecommerceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ecommerce API")
                        .version("v1")
                        .description("Simple ecommerce backend (Spring Boot 3, JPA, MySQL/Testcontainers). "
                                + "Includes global ProblemDetail errors and validation on DTOs."));
    }
}