package com.achernar.identity.profiles.controller.rest;

import com.achernar.identity.profiles.domain.model.ProfileView;
import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(UUID id, String code, String name, String description, boolean active, boolean system,
        long userCount, Instant createdAt) {
    public static ProfileResponse from(ProfileView profile) {
        return new ProfileResponse(profile.id(), profile.code(), profile.name(), profile.description(), profile.active(),
                profile.system(), profile.userCount(), profile.createdAt());
    }
}
