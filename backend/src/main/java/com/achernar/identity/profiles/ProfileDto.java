package com.achernar.identity.profiles;
import java.time.Instant;
import java.util.UUID;
public record ProfileDto(UUID id,String code,String name,String description,boolean active,boolean system,long userCount,Instant createdAt) {}
