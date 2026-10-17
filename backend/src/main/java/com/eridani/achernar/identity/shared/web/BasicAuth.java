package com.eridani.achernar.identity.shared.web;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Interpretação do cabeçalho {@code Authorization: Basic}. Separa no <b>último</b> {@code :}: o segredo é base64url
 * (sem dois-pontos) e a audiência de uma API pode conter {@code :} (ex.: {@code https://api.exemplo.com}).
 */
public final class BasicAuth {

    public record Credentials(String id, String secret) {
    }

    private BasicAuth() {
    }

    public static Optional<Credentials> parse(String header) {
        if (header == null || header.length() < 7 || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(header.substring(6).trim());
            String s = new String(decoded, StandardCharsets.UTF_8);
            int idx = s.lastIndexOf(':');
            if (idx <= 0 || idx == s.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(new Credentials(s.substring(0, idx), s.substring(idx + 1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
