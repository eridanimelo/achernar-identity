package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.entity.ProfileView;
import java.util.UUID;

public interface GetProfileService {
    ProfileView get(UUID id);
}
