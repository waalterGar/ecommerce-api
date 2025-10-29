package com.waalterGar.projects.ecommerce.api.pagination;

import java.util.Set;

public record AllowedSorts(Set<String> fields) {
    public boolean isAllowed(String field) {
        return fields.contains(field);
    }
}
