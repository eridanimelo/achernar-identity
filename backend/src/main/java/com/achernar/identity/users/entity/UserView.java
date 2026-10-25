package com.achernar.identity.users.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserView(UUID id, String name, String email, String role, String status, boolean verified,
        Instant createdAt, List<String> profiles, List<String> assignedProfiles) {
    public static UserView from(UserEntity user) {
        return new UserView(user.id(), user.name(), user.email(), user.role(), user.status(), user.verified(),
                user.createdAt(), user.profiles(), user.assignedProfiles());
    }
}
