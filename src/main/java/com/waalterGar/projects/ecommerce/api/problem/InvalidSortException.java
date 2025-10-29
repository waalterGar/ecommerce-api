package com.waalterGar.projects.ecommerce.api.problem;

import java.util.Set;

public class InvalidSortException extends RuntimeException {
    private final String field;
    private final Set<String> allowed;

    public InvalidSortException(String field, Set<String> allowed) {
        super("Invalid sort field: " + field);
        this.field = field;
        this.allowed = allowed;
    }

    public String field() { return field; }
    public Set<String> allowed() { return allowed; }
}