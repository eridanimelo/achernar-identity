package com.achernar.identity.auth.service;

import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.shared.security.TokenService;
import com.achernar.identity.users.domain.model.UserEntity;
import com.achernar.identity.users.domain.model.UserView;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Audit audit;
    private final TokenService tokens;
    private final String dummy;

    public LoginService(UserRepository users, PasswordEncoder passwords, Audit audit, TokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.audit = audit;
        this.tokens = tokens;
        dummy = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResult login(String email, String password) {
        var found = users.byEmail(AuthActions.normalize(email));
        String hash = found.filter(u -> !u.passwordHash().equals("invited")).map(UserEntity::passwordHash)
                .orElse(dummy);
        boolean match = passwords.matches(password, hash);
        if (!match || found.isEmpty())
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        var user = users.lock(found.get().id()).orElseThrow();
        if (!user.status().equals("ACTIVE") || !user.passwordHash().equals(hash))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        if (!user.verified())
            throw new ApiException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de entrar.");
        if (user.profiles().isEmpty())
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Sua conta não possui perfil ativo. Procure um administrador.");
        audit.record(user.id(), user.id(), "LOGIN");
        return new LoginResult(UserView.from(user), tokens.issue(user));
    }
}
