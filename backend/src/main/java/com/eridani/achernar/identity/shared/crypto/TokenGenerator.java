package com.eridani.achernar.identity.shared.crypto;

import java.security.SecureRandom;
import java.util.Base64;

/** Segredos de alta entropia (256 bits) em base64url sem padding — 43 caracteres (P-23). */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenGenerator() {
    }

    public static String newToken() {
        return newToken(32);
    }

    public static String newToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return ENCODER.encodeToString(buf);
    }

    public static byte[] randomBytes(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return buf;
    }
}
