package com.achernar.identity.users.controller.rest;

import com.achernar.identity.users.domain.model.UserView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String role, String status, boolean verified,
        Instant createdAt, List<String> profiles, List<String> assignedProfiles) {
    public static UserResponse from(UserView user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.role(), user.status(), user.verified(),
                user.createdAt(), user.profiles(), user.assignedProfiles());
    }
}
