package com.eridani.achernar.identity.shared.db;

import com.eridani.achernar.identity.shared.error.ApiException;
import java.util.Map;

/** Ordenação com lista permitida (SEC-32): {@code ?sort=campo,asc|desc}. */
public final class SortSpec {

    private SortSpec() {
    }

    /**
     * @param allowed mapa campo público → expressão SQL segura
     * @param raw     valor recebido (pode ser nulo)
     * @param fallback expressão SQL padrão (já com direção)
     */
    public static String orderBy(Map<String, String> allowed, String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String[] parts = raw.split(",", -1);
        String column = allowed.get(parts[0].trim());
        if (column == null || parts.length > 2) {
            throw ApiException.validation("sort", "INVALID");
        }
        String dir = "ASC";
        if (parts.length == 2) {
            String d = parts[1].trim().toLowerCase(java.util.Locale.ROOT);
            if (d.equals("desc")) {
                dir = "DESC";
            } else if (!d.equals("asc")) {
                throw ApiException.validation("sort", "INVALID");
            }
        }
        return column + " " + dir;
    }
}
