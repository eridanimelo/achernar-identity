package com.achernar.identity.profiles.service.impl;

import com.achernar.identity.profiles.entity.ProfileView;
import com.achernar.identity.profiles.repository.ProfileRepository;
import com.achernar.identity.profiles.service.ProfileLookup;
import com.achernar.identity.profiles.service.UpdateProfileService;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.entity.UserNames;
import com.achernar.identity.users.service.UserAccessService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProfileServiceImpl implements UpdateProfileService {
    private final ProfileRepository profiles;
    private final UserAccessService access;
    private final ProfileLookup lookup;
    private final Audit audit;

    public UpdateProfileServiceImpl(ProfileRepository profiles, UserAccessService access, ProfileLookup lookup,
            Audit audit) {
        this.profiles = profiles;
        this.access = access;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    @Override
    public ProfileView update(UUID actor, UUID id, String name, String description, boolean active) {
        access.lockAdmin(actor);
        var current = lookup.required(id);
        protect(current);
        profiles.update(id, UserNames.normalize(name), description.strip(), active);
        if (current.active() != active)
            access.revokeProfile(id);
        audit.record(actor, id, "PROFILE_UPDATE");
        return lookup.required(id);
    }

    private void protect(ProfileView profile) {
        if (profile.system())
            throw new ApiException(HttpStatus.CONFLICT, "Perfil de sistema protegido contra alteração e exclusão.");
    }
}
