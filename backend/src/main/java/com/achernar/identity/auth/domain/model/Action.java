package com.achernar.identity.auth.domain.model;

import java.util.UUID;

public record Action(UUID id, UUID userId) {
}
