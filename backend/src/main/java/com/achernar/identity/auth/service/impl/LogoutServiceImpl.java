package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.service.LogoutService;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.users.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutServiceImpl implements LogoutService {
    private final UserRepository users;
    private final Audit audit;

    public LogoutServiceImpl(UserRepository users, Audit audit) {
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    @Override
    public void logout(UUID id) {
        users.revoke(id);
        audit.record(id, id, "LOGOUT");
    }
}
