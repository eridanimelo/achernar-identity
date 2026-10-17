package com.eridani.achernar.identity.shared.error;

/** Item de {@code errors[]} (api-conventions §1.1). */
public record FieldError(String field, String code, String message) {

    public static FieldError of(String field, String code) {
        return new FieldError(field, code, null);
    }
}
