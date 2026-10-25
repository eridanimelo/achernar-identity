package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.entity.ProfileView;
import java.util.UUID;

public interface CreateProfileService {
    ProfileView create(UUID actor, String code, String name, String description);
}
