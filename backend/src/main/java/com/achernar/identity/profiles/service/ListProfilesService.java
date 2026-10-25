package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListProfilesService {
    private final ProfileRepository profiles;

    public ListProfilesService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProfilePage list(String q, Boolean active, int page, int size) {
        long total = profiles.count(q, active);
        return new ProfilePage(profiles.list(q, active, page, size), page, size, total, (total + size - 1) / size);
    }
}
