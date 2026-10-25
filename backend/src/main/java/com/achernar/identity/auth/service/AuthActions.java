package com.achernar.identity.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public interface AuthActions {
    static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    void requestLink(String email, String kind);

    void sendLink(UUID id, String kind);

    void consume(String token, String kind, String password);

    void removeActions(UUID userId);
}
