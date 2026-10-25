package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.model.ProfileView;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProfileService {
    private final ProfileLookup lookup;

    public GetProfileService(ProfileLookup lookup) {
        this.lookup = lookup;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProfileView get(UUID id) {
        return lookup.required(id);
    }
}
