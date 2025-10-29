package com.waalterGar.projects.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pagination")
public record PaginationProperties(
        int defaultSize,
        int maxSize,
        Defaults defaults
) {
    public record Defaults(String products, String orders, String customers) {}
}
