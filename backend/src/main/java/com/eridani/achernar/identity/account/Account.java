package com.eridani.achernar.identity.account;

import java.time.Instant;
import java.util.UUID;

/** Conta (RN-ACC-*). Objeto de leitura imutável. */
public record Account(
        UUID id,
        String emailDisplay,
        String emailNormalized,
        AccountStatus status,
        AccountRole role,
        Instant emailVerifiedAt,
        String locale,
        Instant disabledAt,
        String disabledReason,
        Instant anonymizedAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public boolean active() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean admin() {
        return role == AccountRole.ADMIN;
    }

    public boolean anonymized() {
        return anonymizedAt != null;
    }
}
