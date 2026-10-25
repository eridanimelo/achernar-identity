package com.achernar.identity.users.service.impl;

import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.profiles.service.ProfileResolver;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.entity.UserNames;
import com.achernar.identity.users.entity.UserView;
import com.achernar.identity.users.repository.UserRepository;
import com.achernar.identity.users.service.UpdateUserService;
import com.achernar.identity.users.service.UserAccessService;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserServiceImpl implements UpdateUserService {
    private final UserRepository users;
    private final ProfileResolver profiles;
    private final UserAccessService access;
    private final AuthActions actions;
    private final Audit audit;

    public UpdateUserServiceImpl(UserRepository users, ProfileResolver profiles, UserAccessService access,
            AuthActions actions, Audit audit) {
        this.users = users;
        this.profiles = profiles;
        this.access = access;
        this.actions = actions;
        this.audit = audit;
    }

    @Transactional
    @Override
    public UserView update(UUID actor, UUID target, String name, String email, List<String> codes, String role,
            String status) {
        access.lockAdmin(actor);
        var user = access.lockTarget(actor, target);
        if (role != null && codes != null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use profiles ou role legado, nunca ambos.");
        List<String> nextCodes = codes != null ? codes : role != null ? List.of(role) : null;
        var ids = nextCodes == null ? null : profiles.resolve(nextCodes);
        String nextStatus = status == null ? user.status() : status;
        String nextEmail = email == null ? user.email() : AuthActions.normalize(email);
        boolean emailChanged = !nextEmail.equals(user.email());
        access.protect(user, nextCodes == null ? user.profiles() : nextCodes, nextStatus, emailChanged);
        if (emailChanged && users.byEmail(nextEmail).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        if (ids != null)
            users.assign(target, ids);
        try {
            users.update(target, name == null ? user.name() : UserNames.normalize(name), nextEmail, nextStatus,
                    user.verified() && !emailChanged);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        if (emailChanged) {
            actions.removeActions(target);
            actions.sendLink(target, user.passwordHash().equals("invited") ? "INVITE" : "VERIFY");
        }
        audit.record(actor, target, "ADMIN_CHANGE");
        return UserView.from(users.byId(target).orElseThrow());
    }
}
