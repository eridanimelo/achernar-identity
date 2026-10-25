package com.achernar.identity.profiles.service.impl;

import com.achernar.identity.profiles.entity.ProfileView;
import com.achernar.identity.profiles.exception.ProfileNotFoundException;
import com.achernar.identity.profiles.repository.ProfileRepository;
import com.achernar.identity.profiles.service.ProfileLookup;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileLookupImpl implements ProfileLookup {
    private final ProfileRepository profiles;

    public ProfileLookupImpl(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public ProfileView required(UUID id) {
        return profiles.byId(id).orElseThrow(ProfileNotFoundException::new);
    }
}
