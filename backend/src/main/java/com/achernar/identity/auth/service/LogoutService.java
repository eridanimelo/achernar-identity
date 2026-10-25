package com.achernar.identity.auth.service;

import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService {
    private final UserRepository users;
    private final Audit audit;

    public LogoutService(UserRepository users, Audit audit) {
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public void logout(UUID id) {
        users.revoke(id);
        audit.record(id, id, "LOGOUT");
    }
}
