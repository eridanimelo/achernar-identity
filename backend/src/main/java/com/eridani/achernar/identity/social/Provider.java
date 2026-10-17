package com.eridani.achernar.identity.social;

import java.util.Locale;
import java.util.Optional;

public enum Provider {
    GOOGLE("google"), FACEBOOK("facebook");

    private final String slug;

    Provider(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public String amr() {
        return slug;
    }

    public static Optional<Provider> fromSlug(String s) {
        if (s == null) {
            return Optional.empty();
        }
        String v = s.toLowerCase(Locale.ROOT);
        for (Provider p : values()) {
            if (p.slug.equals(v)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
