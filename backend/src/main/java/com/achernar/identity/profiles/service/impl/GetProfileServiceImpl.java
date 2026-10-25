package com.achernar.identity.profiles.service.impl;

import com.achernar.identity.profiles.entity.ProfileView;
import com.achernar.identity.profiles.service.GetProfileService;
import com.achernar.identity.profiles.service.ProfileLookup;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProfileServiceImpl implements GetProfileService {
    private final ProfileLookup lookup;

    public GetProfileServiceImpl(ProfileLookup lookup) {
        this.lookup = lookup;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ProfileView get(UUID id) {
        return lookup.required(id);
    }
}
