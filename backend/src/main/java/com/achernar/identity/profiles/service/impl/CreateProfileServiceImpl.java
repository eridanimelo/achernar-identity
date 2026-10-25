package com.achernar.identity.profiles.service.impl;

import com.achernar.identity.profiles.entity.ProfileView;
import com.achernar.identity.profiles.repository.ProfileRepository;
import com.achernar.identity.profiles.service.CreateProfileService;
import com.achernar.identity.profiles.service.ProfileLookup;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.entity.UserNames;
import com.achernar.identity.users.service.UserAccessService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProfileServiceImpl implements CreateProfileService {
    private final ProfileRepository profiles;
    private final UserAccessService access;
    private final ProfileLookup lookup;
    private final Audit audit;

    public CreateProfileServiceImpl(ProfileRepository profiles, UserAccessService access, ProfileLookup lookup,
            Audit audit) {
        this.profiles = profiles;
        this.access = access;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    @Override
    public ProfileView create(UUID actor, String code, String name, String description) {
        access.lockAdmin(actor);
        UUID id = UUID.randomUUID();
        if (!profiles.create(id, code, UserNames.normalize(name), description.strip()))
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um perfil com esse código.");
        audit.record(actor, id, "PROFILE_CREATE");
        return lookup.required(id);
    }
}
