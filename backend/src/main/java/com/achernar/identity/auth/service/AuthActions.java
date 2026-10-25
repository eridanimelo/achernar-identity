package com.achernar.identity.auth.service;

import com.achernar.identity.auth.domain.repository.ActionRepository;
import com.achernar.identity.shared.audit.Audit;
import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.shared.mail.MailQueue;
import com.achernar.identity.users.domain.model.UserEntity;
import com.achernar.identity.users.domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthActions {
    private final UserRepository users;
    private final ActionRepository actions;
    private final MailQueue mail;
    private final PasswordEncoder passwords;
    private final Audit audit;
    private final String actionUrl;

    public AuthActions(UserRepository users, ActionRepository actions, MailQueue mail, PasswordEncoder passwords,
            Audit audit, @Value("${app.action-url}") String actionUrl) {
        this.users = users;
        this.actions = actions;
        this.mail = mail;
        this.passwords = passwords;
        this.audit = audit;
        this.actionUrl = actionUrl;
    }

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void requestLink(String email, String kind) {
        users.byEmail(normalize(email)).ifPresent(found -> {
            var user = users.lock(found.id()).orElseThrow();
            if (user.status().equals("ACTIVE") && (kind.equals("RESET") ? user.verified() : !user.verified()))
                send(user, kind.equals("VERIFY") && user.passwordHash().equals("invited") ? "INVITE" : kind);
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void sendLink(UUID id, String kind) {
        send(users.byId(id).orElseThrow(), kind);
    }

    @Transactional
    public void consume(String token, String kind, String password) {
        var action = actions.find(hash(token), kind).orElseThrow(AuthActions::invalidLink);
        var user = users.lock(action.userId()).orElseThrow(AuthActions::invalidLink);
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

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeActions(UUID userId) {
        actions.remove(userId);
    }

    private void send(UserEntity user, String kind) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        actions.invalidate(user.id(), kind);
        actions.create(user.id(), hash(token), kind, Instant.now().plusSeconds(kind.equals("RESET") ? 1800 : 86400));
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
