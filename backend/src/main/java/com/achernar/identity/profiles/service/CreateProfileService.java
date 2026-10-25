package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.model.ProfileView;
import com.achernar.identity.profiles.domain.repository.ProfileRepository;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.domain.model.UserNames;
import com.achernar.identity.users.service.UserAccessService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProfileService {
    private final ProfileRepository profiles;
    private final UserAccessService access;
    private final ProfileLookup lookup;
    private final Audit audit;

    public CreateProfileService(ProfileRepository profiles, UserAccessService access, ProfileLookup lookup,
            Audit audit) {
        this.profiles = profiles;
        this.access = access;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public ProfileView create(UUID actor, String code, String name, String description) {
        access.lockAdmin(actor);
        UUID id = UUID.randomUUID();
        if (!profiles.create(id, code, UserNames.normalize(name), description.strip()))
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um perfil com esse código.");
        audit.record(actor, id, "PROFILE_CREATE");
        return lookup.required(id);
    }
}
