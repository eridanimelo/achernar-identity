package com.achernar.identity.profiles.service.impl;

import com.achernar.identity.profiles.dto.ProfilePage;
import com.achernar.identity.profiles.repository.ProfileRepository;
import com.achernar.identity.profiles.service.ListProfilesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListProfilesServiceImpl implements ListProfilesService {
    private final ProfileRepository profiles;

    public ListProfilesServiceImpl(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public ProfilePage list(String q, Boolean active, int page, int size) {
        long total = profiles.count(q, active);
        return new ProfilePage(profiles.list(q, active, page, size), page, size, total, (total + size - 1) / size);
    }
}
