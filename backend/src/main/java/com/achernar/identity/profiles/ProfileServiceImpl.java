package com.achernar.identity.profiles;

import com.achernar.identity.users.*;
import com.achernar.identity.shared.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profiles;
    private final UserAccessService access;
    private final Audit audit;

    public ProfileServiceImpl(ProfileRepository profiles, UserAccessService access, Audit audit) {
        this.profiles = profiles;
        this.access = access;
        this.audit = audit;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page list(String q, Boolean active, int page, int size) {
        long total = profiles.count(q, active);
        return new Page(profiles.list(q, active, page, size), page, size, total, (total + size - 1) / size);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProfileDto get(UUID id) {
        return required(id);
    }

    private ProfileDto required(UUID id) {
        return profiles.byId(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil não encontrado."));
    }

    @Override
    @Transactional
    public ProfileDto create(UUID actor, String code, String name, String description) {
        access.lockAdmin(actor);
        UUID id = UUID.randomUUID();
        if (!profiles.create(id, code, UserNames.normalize(name), description.strip()))
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um perfil com esse código.");
        audit.record(actor, id, "PROFILE_CREATE");
        return required(id);
    }

    @Override
    @Transactional
    public ProfileDto update(UUID actor, UUID id, String name, String description, boolean active) {
        access.lockAdmin(actor);
        var current = required(id);
        protect(current);
        profiles.update(id, UserNames.normalize(name), description.strip(), active);
        if (current.active() != active)
            access.revokeProfile(id);
        audit.record(actor, id, "PROFILE_UPDATE");
        return required(id);
    }

    @Override
    @Transactional
    public void delete(UUID actor, UUID id) {
        access.lockAdmin(actor);
        var current = required(id);
        protect(current);
        if (current.userCount() > 0)
            throw new ApiException(HttpStatus.CONFLICT,
                    "Perfil vinculado a usuários. Remova os vínculos antes de excluir.");
        profiles.delete(id);
        audit.record(actor, id, "PROFILE_DELETE");
    }

    private void protect(ProfileDto profile) {
        if (profile.system())
            throw new ApiException(HttpStatus.CONFLICT, "Perfil de sistema protegido contra alteração e exclusão.");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> resolve(List<String> codes) {
        if (codes == null || codes.isEmpty() || codes.size() > 20 || new HashSet<>(codes).size() != codes.size())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecione de 1 a 20 perfis, sem repetição.");
        var found = profiles.resolve(codes);
        if (found.size() != codes.size())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Todos os perfis devem existir e estar ativos.");
        return found.stream().map(ProfileDto::id).toList();
    }
}
