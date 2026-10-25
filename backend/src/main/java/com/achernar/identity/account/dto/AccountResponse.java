package com.achernar.identity.account.dto;

import com.achernar.identity.users.entity.UserView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountResponse(UUID id, String name, String email, String role, String status, boolean verified,
        Instant createdAt, List<String> profiles, List<String> assignedProfiles) {
    public static AccountResponse from(UserView user) {
        return new AccountResponse(user.id(), user.name(), user.email(), user.role(), user.status(), user.verified(),
                user.createdAt(), user.profiles(), user.assignedProfiles());
    }
}
