package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.dto.ProfilePage;

public interface ListProfilesService {
    ProfilePage list(String q, Boolean active, int page, int size);
}
