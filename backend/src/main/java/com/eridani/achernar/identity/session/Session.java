package com.eridani.achernar.identity.session;

import java.time.Instant;
import java.util.UUID;

/** Sessão por aplicação (RN-SES-01); {@code id} é o {@code sid} do access token. */
public record Session(UUID id, UUID accountId, UUID applicationId, String amr, Instant createdAt, Instant authTime,
                      Instant lastUsedAt, Instant expiresAt, Instant lastReauthAt, Instant revokedAt,
                      String revokeReason, String ip, String userAgent) {

    public boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
