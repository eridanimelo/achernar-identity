package com.achernar.identity.users;
import java.time.Instant;
import java.util.*;
public record UserDto(UUID id, String name, String email, String role, String status, boolean verified,
    Instant createdAt, List<String> profiles, List<String> assignedProfiles) {
    public static UserDto from(UserEntity u) { return new UserDto(u.id(),u.name(),u.email(),u.role(),u.status(),u.verified(),u.createdAt(),u.profiles(),u.assignedProfiles()); }
}
