package com.eridani.achernar.identity.registry;

import java.time.Instant;
import java.util.UUID;

/** API consumidora (RN-APP-14). */
public record ResourceApi(UUID id, String audience, String name, String description, RegistryStatus status,
                          Instant createdAt, Instant updatedAt, long version) {

    public boolean active() {
        return status == RegistryStatus.ACTIVE;
    }
}
