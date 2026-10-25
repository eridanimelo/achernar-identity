package com.achernar.identity.users.service;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.profiles.service.ProfileResolver;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.domain.model.UserNames;
import com.achernar.identity.users.domain.model.UserView;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserService {
    private final UserRepository users;
    private final ProfileResolver profiles;
    private final UserAccessService access;
    private final AuthActions actions;
    private final Audit audit;

    public CreateUserService(UserRepository users, ProfileResolver profiles, UserAccessService access,
            AuthActions actions, Audit audit) {
        this.users = users;
        this.profiles = profiles;
        this.access = access;
        this.actions = actions;
        this.audit = audit;
    }

    @Transactional
    public UserView create(UUID actor, String name, String email, List<String> codes) {
        access.lockAdmin(actor);
        var ids = profiles.resolve(codes);
        UUID id = UUID.randomUUID();
        if (!users.create(id, UserNames.normalize(name), AuthActions.normalize(email), "invited", "USER", false))
            throw new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        users.assign(id, ids);
        actions.sendLink(id, "INVITE");
        audit.record(actor, id, "ADMIN_CREATE");
        return UserView.from(users.byId(id).orElseThrow());
    }
}
