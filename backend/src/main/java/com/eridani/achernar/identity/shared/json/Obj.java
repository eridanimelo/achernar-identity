package com.eridani.achernar.identity.shared.json;

import java.util.LinkedHashMap;

/** Mapa ordenado para montar respostas JSON snake_case; valores nulos são omitidos. */
public class Obj extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public static Obj of() {
        return new Obj();
    }

    public Obj with(String key, Object value) {
        if (value != null) {
            put(key, value);
        }
        return this;
    }

    /** Inclui a chave mesmo com valor nulo (ex.: {@code next_cursor}). */
    public Obj withNullable(String key, Object value) {
        put(key, value);
        return this;
    }
}
