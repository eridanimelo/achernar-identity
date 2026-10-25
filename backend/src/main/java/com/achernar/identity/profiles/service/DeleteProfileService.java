package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.model.ProfileView;
import com.achernar.identity.profiles.domain.repository.ProfileRepository;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.service.UserAccessService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProfileService {
    private final ProfileRepository profiles;
    private final UserAccessService access;
    private final ProfileLookup lookup;
    private final Audit audit;

    public DeleteProfileService(ProfileRepository profiles, UserAccessService access, ProfileLookup lookup,
            Audit audit) {
        this.profiles = profiles;
        this.access = access;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public void delete(UUID actor, UUID id) {
        access.lockAdmin(actor);
        var current = lookup.required(id);
        protect(current);
        if (current.userCount() > 0)
            throw new ApiException(HttpStatus.CONFLICT,
                    "Perfil vinculado a usuários. Remova os vínculos antes de excluir.");
        profiles.delete(id);
        audit.record(actor, id, "PROFILE_DELETE");
    }

    private void protect(ProfileView profile) {
        if (profile.system())
            throw new ApiException(HttpStatus.CONFLICT, "Perfil de sistema protegido contra alteração e exclusão.");
    }
}
