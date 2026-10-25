package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.exception.ProfileNotFoundException;
import com.achernar.identity.profiles.domain.model.ProfileView;
import com.achernar.identity.profiles.domain.repository.ProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileLookup {
    private final ProfileRepository profiles;

    public ProfileLookup(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    public ProfileView required(UUID id) {
        return profiles.byId(id).orElseThrow(ProfileNotFoundException::new);
    }
}
