package com.eridani.achernar.identity.shared.web;

import java.util.Locale;

/** Idiomas suportados: {@code pt-BR} (padrão) e {@code en} (D-37). */
public final class LocaleSupport {

    public static final String PT_BR = "pt-BR";
    public static final String EN = "en";

    private LocaleSupport() {
    }

    /** Normaliza um valor de locale/Accept-Language para um dos suportados; {@code fallback} se nenhum. */
    public static String resolve(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        for (String part : value.split(",")) {
            String tag = part.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (tag.equals("en") || tag.startsWith("en-")) {
                return EN;
            }
            if (tag.equals("pt") || tag.startsWith("pt-")) {
                return PT_BR;
            }
        }
        return fallback;
    }

    public static boolean isSupported(String tag) {
        return PT_BR.equals(tag) || EN.equals(tag);
    }

    public static Locale toJavaLocale(String tag) {
        return EN.equals(tag) ? Locale.ENGLISH : Locale.forLanguageTag("pt-BR");
    }
}
