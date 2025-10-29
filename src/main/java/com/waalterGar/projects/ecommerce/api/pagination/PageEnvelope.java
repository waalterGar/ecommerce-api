package com.waalterGar.projects.ecommerce.api.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageEnvelope<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        List<String> sort
) {
    public static <T> PageEnvelope<T> of(Page<T> page) {
        return new PageEnvelope<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getSort().stream()
                        .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                        .toList()
        );
    }
}
