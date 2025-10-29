package com.waalterGar.projects.ecommerce.api.pagination;


import java.util.ArrayList;
import java.util.List;

public final class SortParser {
    private SortParser() {}

    public static List<SortDirective> parse(List<String> raw) {
        List<SortDirective> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return result;

        for (String s : raw) {
            String[] parts = s.split(",", -1);
            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid sort: " + s);

            String field = parts[0].trim();
            String dir = parts[1].trim().toLowerCase();

            if (field.isEmpty() || !(dir.equals("asc") || dir.equals("desc")))
                throw new IllegalArgumentException("Invalid sort: " + s);

            result.add(new SortDirective(field, dir.equals("asc")));
        }
        return result;
    }
}
