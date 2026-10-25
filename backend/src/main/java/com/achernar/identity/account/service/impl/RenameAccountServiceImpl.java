package com.achernar.identity.account.service.impl;

import com.achernar.identity.account.service.RenameAccountService;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.entity.UserNames;
import com.achernar.identity.users.entity.UserView;
import com.achernar.identity.users.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenameAccountServiceImpl implements RenameAccountService {
    private final UserRepository users;
    private final Audit audit;

    public RenameAccountServiceImpl(UserRepository users, Audit audit) {
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    @Override
    public UserView rename(UUID id, String name) {
        var user = users.lock(id).orElseThrow();
        if (!user.status().equals("ACTIVE"))
            throw new ApiException(HttpStatus.FORBIDDEN, "Conta indisponível.");
        users.rename(id, UserNames.normalize(name));
        audit.record(id, id, "RENAME");
        return UserView.from(users.byId(id).orElseThrow());
    }
}
