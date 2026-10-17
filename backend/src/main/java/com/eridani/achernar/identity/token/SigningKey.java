package com.eridani.achernar.identity.token;

import java.time.Instant;
import java.util.UUID;

/** Linha de {@code signing_key}; {@code privateKeyEnc} é nulo após a remoção (D-25). */
public record SigningKey(UUID id, String kid, String publicJwk, byte[] privateKeyEnc, KeyStatus status,
                         Instant createdAt, Instant publishAt, Instant activateAt, Instant retireAt,
                         Instant removeAt, String removalReason) {
}
