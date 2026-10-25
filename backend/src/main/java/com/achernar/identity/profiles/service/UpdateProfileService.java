package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.entity.ProfileView;
import java.util.UUID;

public interface UpdateProfileService {
    ProfileView update(UUID actor, UUID id, String name, String description, boolean active);
}
