package com.eridani.achernar.identity.shared.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Página de resultados: {@code {items,page,size,total}} (api-conventions §1). */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 100;

    public static int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(MAX_SIZE, size));
    }

    public static int clampPage(Integer page) {
        return page == null ? 0 : Math.max(0, page);
    }

    public <R> Map<String, Object> toBody(Function<T, R> mapper) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", items.stream().map(mapper).toList());
        m.put("page", page);
        m.put("size", size);
        m.put("total", total);
        return m;
    }
}
