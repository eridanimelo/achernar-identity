package com.achernar.identity.profiles.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProfileView(UUID id, String code, String name, String description, boolean active, boolean system,
        long userCount, Instant createdAt) {
}
