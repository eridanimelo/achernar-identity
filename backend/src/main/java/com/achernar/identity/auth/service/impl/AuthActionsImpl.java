package com.achernar.identity.auth.service.impl;

import com.achernar.identity.auth.repository.ActionRepository;
import com.achernar.identity.auth.service.AuthActions;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.shared.mail.MailQueue;
import com.achernar.identity.users.entity.UserEntity;
import com.achernar.identity.users.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthActionsImpl implements AuthActions {
    private final UserRepository users;
    private final ActionRepository actions;
    private final MailQueue mail;
    private final PasswordEncoder passwords;
    private final Audit audit;
    private final String actionUrl;

    public AuthActionsImpl(UserRepository users, ActionRepository actions, MailQueue mail, PasswordEncoder passwords,
            Audit audit, @Value("${app.action-url}") String actionUrl) {
        this.users = users;
        this.actions = actions;
        this.mail = mail;
        this.passwords = passwords;
        this.audit = audit;
        this.actionUrl = actionUrl;
    }

    @Override
    @Transactional
    public void requestLink(String email, String kind) {
        users.byEmail(AuthActions.normalize(email)).ifPresent(found -> {
            var user = users.lock(found.id()).orElseThrow();
            if (user.status().equals("ACTIVE") && (kind.equals("RESET") ? user.verified() : !user.verified()))
                send(user, kind.equals("VERIFY") && user.passwordHash().equals("invited") ? "INVITE" : kind);
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void sendLink(UUID id, String kind) {
        send(users.byId(id).orElseThrow(), kind);
    }

    @Override
    @Transactional
    public void consume(String token, String kind, String password) {
        var action = actions.find(AuthActions.hash(token), kind).orElseThrow(AuthActionsImpl::invalidLink);
        var user = users.lock(action.userId()).orElseThrow(AuthActionsImpl::invalidLink);
        if (!user.status().equals("ACTIVE") || !actions.consume(action.id()))
            throw invalidLink();
        if (kind.equals("RESET") || kind.equals("INVITE")) {
            users.reset(user.id(), passwords.encode(password));
            actions.invalidate(user.id(), "RESET");
            if (kind.equals("INVITE"))
                users.verify(user.id());
        } else
            users.verify(user.id());
        audit.record(user.id(), user.id(),
                kind.equals("RESET") ? "RESET_PASSWORD" : kind.equals("INVITE") ? "ACTIVATE_ACCOUNT" : "VERIFY_EMAIL");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeActions(UUID userId) {
        actions.remove(userId);
    }

    private void send(UserEntity user, String kind) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        actions.invalidate(user.id(), kind);
        actions.create(user.id(), AuthActions.hash(token), kind, Instant.now().plusSeconds(kind.equals("RESET") ? 1800 : 86400));
        String path = kind.equals("RESET") ? "/auth/reset-password"
                : kind.equals("INVITE") ? "/auth/activate" : "/auth/verify";
        mail.enqueue(user.email(),
                kind.equals("RESET") ? "Redefina sua senha — Achernar"
                        : kind.equals("INVITE") ? "Ative sua conta — Achernar" : "Confirme seu e-mail — Achernar",
                "Olá, " + user.name() + ".\n\nAbra o link e confirme a operação:\n" + actionUrl + path + "#token="
                        + token + "\n\nSe você não solicitou, ignore esta mensagem.");
    }

    private static ApiException invalidLink() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Link inválido, expirado ou já utilizado. Solicite um novo.");
    }
}
