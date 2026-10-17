package com.eridani.achernar.identity.shared.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Time {

    private Time() {
    }

    /** ISO-8601 UTC com {@code Z}, sem frações (D-42, D-51). */
    public static String iso(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
