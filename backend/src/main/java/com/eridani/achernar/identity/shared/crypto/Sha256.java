package com.eridani.achernar.identity.shared.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 em hexadecimal minúsculo (64 caracteres) — formato de {@code token_hash}/{@code secret_hash}. */
public final class Sha256 {

    private Sha256() {
    }

    public static byte[] digest(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public static byte[] digest(String value) {
        return digest(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String hex(String value) {
        return HexFormat.of().formatHex(digest(value));
    }
}
