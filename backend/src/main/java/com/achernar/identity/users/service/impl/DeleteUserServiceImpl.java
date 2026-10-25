package com.achernar.identity.users.service.impl;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.users.repository.UserRepository;
import com.achernar.identity.users.service.DeleteUserService;
import com.achernar.identity.users.service.UserAccessService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteUserServiceImpl implements DeleteUserService {
    private final UserRepository users;
    private final UserAccessService access;
    private final AuthActions actions;
    private final Audit audit;

    public DeleteUserServiceImpl(UserRepository users, UserAccessService access, AuthActions actions, Audit audit) {
        this.users = users;
        this.access = access;
        this.actions = actions;
        this.audit = audit;
    }

    @Transactional
    @Override
    public void delete(UUID actor, UUID target) {
        access.lockAdmin(actor);
        var user = access.lockTarget(actor, target);
        access.protect(user, user.profiles(), "DELETED", false);
        users.delete(target);
        actions.removeActions(target);
        audit.record(actor, target, "ADMIN_DELETE");
    }
}
