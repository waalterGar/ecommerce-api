package com.waalterGar.projects.ecommerce.api.pagination;

import com.waalterGar.projects.ecommerce.api.problem.InvalidSortException;
import java.util.List;

public final class SortValidator {
    private SortValidator() {}

    public static void ensureAllowed(List<SortDirective> directives, AllowedSorts whitelist) {
        for (SortDirective d : directives) {
            if (!whitelist.isAllowed(d.field())) {
                throw new InvalidSortException(d.field(), whitelist.fields());
            }
        }
    }
}
