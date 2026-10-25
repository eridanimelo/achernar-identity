package com.achernar.identity.auth.service;

import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.users.domain.model.UserNames;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuthActions actions;
    private final Audit audit;

    public RegisterService(UserRepository users, PasswordEncoder passwords, AuthActions actions, Audit audit) {
        this.users = users;
        this.passwords = passwords;
        this.actions = actions;
        this.audit = audit;
    }

    @Transactional
    public void register(String name, String email, String password) {
        UUID id = UUID.randomUUID();
        if (users.create(id, UserNames.normalize(name), AuthActions.normalize(email), passwords.encode(password),
                "USER", false)) {
            actions.sendLink(id, "VERIFY");
            audit.record(id, id, "REGISTER");
        }
    }
}
