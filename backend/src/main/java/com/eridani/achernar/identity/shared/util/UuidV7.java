package com.eridani.achernar.identity.shared.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/** UUID v7 (ordenável por tempo) para tabelas de alto volume — D-29. */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate(Instant now) {
        long millis = now.toEpochMilli();
        long randA = RANDOM.nextInt(1 << 12);
        long msb = (millis << 16) | (0x7L << 12) | randA;
        long randB = RANDOM.nextLong();
        long lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
