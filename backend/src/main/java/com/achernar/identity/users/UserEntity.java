package com.achernar.identity.users;
import java.time.Instant;
import java.util.*;
public record UserEntity(UUID id, String name, String email, String passwordHash, String role,
    String status, boolean verified, long sessionVersion, Instant createdAt, List<String> profiles, List<String> assignedProfiles) {
    public UserEntity(UUID id,String name,String email,String hash,String role,String status,boolean verified,long version,Instant createdAt) {
        this(id,name,email,hash,role,status,verified,version,createdAt,List.of(role),List.of(role));
    }
}
