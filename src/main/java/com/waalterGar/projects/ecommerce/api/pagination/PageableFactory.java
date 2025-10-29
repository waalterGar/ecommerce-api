package com.waalterGar.projects.ecommerce.api.pagination;


import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public final class PageableFactory {
    private PageableFactory() {}

    public static Pageable from(
            int page,
            int size,
            List<SortDirective> directives,
            AllowedSorts whitelist,
            String defaultSort,
            PaginationProperties props
    ) {
        int clampedSize = Math.max(1, Math.min(size, props.maxSize()));
        Sort sort = (directives == null || directives.isEmpty())
                ? parseDefault(defaultSort)
                : toSpringSort(directives);
        return PageRequest.of(Math.max(0, page), clampedSize, sort);
    }

    private static Sort parseDefault(String def) {
        if (def == null || def.isBlank()) return Sort.unsorted();
        String[] parts = def.split(",", -1);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        return dir.equals("desc") ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    private static Sort toSpringSort(List<SortDirective> directives) {
        Sort sort = Sort.unsorted();
        for (SortDirective d : directives) {
            Sort next = d.asc()
                    ? Sort.by(d.field()).ascending()
                    : Sort.by(d.field()).descending();
            sort = sort.and(next);
        }
        return sort;
    }
}
