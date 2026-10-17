package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.account.AccountStatus;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.credential.PasswordCredentialRepository;
import com.eridani.achernar.identity.credential.PasswordHasher;
import com.eridani.achernar.identity.mail.ActionLinks;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.ratelimit.LoginLockService;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.Ips;
import com.eridani.achernar.identity.token.JwtIssuer;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** UC-020 — autenticação por e-mail e senha (RN-AUT-01…11, D-13, D-48). */
@Service
public class LoginService {

    static final int MAX_PASSWORD = 128;

    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final PasswordHasher hasher;
    private final LoginLockService locks;
    private final RateLimiter limiter;
    private final SessionService sessions;
    private final JwtIssuer jwt;
    private final AudiencePolicy audiencePolicy;
    private final TokenResponses responses;
    private final SecurityEventRecorder events;
    private final MailService mail;
    private final ActionLinks links;
    private final Clock clock;
    private final TransactionTemplate tx;

    public LoginService(AccountRepository accounts, PasswordCredentialRepository credentials,
                        PasswordHasher hasher, LoginLockService locks, RateLimiter limiter,
                        SessionService sessions, JwtIssuer jwt, AudiencePolicy audiencePolicy,
                        TokenResponses responses, SecurityEventRecorder events, MailService mail,
                        ActionLinks links, Clock clock, TransactionTemplate tx) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.hasher = hasher;
        this.locks = locks;
        this.limiter = limiter;
        this.sessions = sessions;
        this.jwt = jwt;
        this.audiencePolicy = audiencePolicy;
        this.responses = responses;
        this.events = events;
        this.mail = mail;
        this.links = links;
        this.clock = clock;
        this.tx = tx;
    }

    public TokenResponses.Result login(ClientContext client, String email, String password, List<String> audiences) {
        limiter.checkAll(Ips.subject(), List.of(RateRule.AUTH_IP_MINUTE, RateRule.AUTH_IP_HOUR));
        Application app = client.application();
        EmailAddress.Normalized parsed = EmailAddress.parse(email)
                .orElseThrow(() -> ApiException.validation("email", "INVALID_FORMAT"));
        if (password == null || password.isEmpty() || password.length() > MAX_PASSWORD) {
            throw ApiException.validation("password", password == null || password.isEmpty() ? "REQUIRED" : "TOO_LONG");
        }
        AudiencePolicy.sanitize(audiences);

        String key = parsed.normalized();
        Account account = accounts.findByEmail(key).filter(a -> !a.anonymized()).orElse(null);

        if (locks.remainingLockSeconds(key) > 0) {
            hasher.verifyDummy(password); // tempo equivalente (D-48)
            failed(app, account, "THROTTLED");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        Optional<PasswordCredentialRepository.Credential> credential =
                account == null ? Optional.empty() : credentials.find(account.id());
        boolean ok;
        if (credential.isPresent()) {
            ok = hasher.verify(password, credential.get().hash());
        } else {
            hasher.verifyDummy(password);
            ok = false;
        }
        if (!ok) {
            LoginLockService.Failure f = locks.registerFailure(key);
            failed(app, account, account == null || credential.isEmpty() ? "UNKNOWN_ACCOUNT" : "BAD_PASSWORD");
            if (f.newlyLocked()) {
                locked(app, account, f);
            }
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Senha provada: agora os estados da conta podem ser revelados (SEC-08).
        if (account.status() == AccountStatus.PENDING_VERIFICATION) {
            events.recordIndependent(EventType.LOGIN_BLOCKED_UNVERIFIED, account.id(), app.clientId(), Map.of());
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (account.status() == AccountStatus.DISABLED) {
            events.recordIndependent(EventType.LOGIN_BLOCKED_DISABLED, account.id(), app.clientId(), Map.of());
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        List<String> granted = audiencePolicy.forLogin(app, audiences);

        return tx.execute(status -> {
            Instant now = clock.instant();
            SessionService.Created created = sessions.create(account.id(), app, "pwd", granted);
            if (hasher.needsRehash(credential.get().hash())) {
                credentials.rehash(account.id(), hasher.hash(password));
            }
            JwtIssuer.Issued access = jwt.issue(new JwtIssuer.Request(account.id(), created.session().id(),
                    app.clientId(), granted, "pwd", created.session().authTime(), account.admin(),
                    app.accessTokenTtlSeconds()));
            events.record(AuditEvent.of(EventType.LOGIN_SUCCEEDED).account(account.id()).client(app.clientId())
                    .session(created.session().id()).detail("amr", "pwd"));
            events.record(AuditEvent.of(EventType.TOKEN_ISSUED).account(account.id()).client(app.clientId())
                    .session(created.session().id()).detail("audiences", granted));
            return responses.build(app, access, created.session().id(), granted, created.refreshToken(),
                    created.refreshExpiresAt(), now, true);
        });
    }

    private void failed(Application app, Account account, String reason) {
        events.recordIndependent(EventType.LOGIN_FAILED, account == null ? null : account.id(), app.clientId(),
                Map.of("reason", reason));
    }

    private void locked(Application app, Account account, LoginLockService.Failure f) {
        events.recordIndependent(EventType.ACCOUNT_LOCKED_THROTTLE, account == null ? null : account.id(),
                app.clientId(), Map.of("lock_seconds", f.lockDuration().toSeconds()));
        if (account != null) { // TPL-08 só ao titular existente (1 por bloqueio)
            tx.executeWithoutResult(s -> {
                Map<String, String> vars = new HashMap<>();
                vars.put("app_name", app.name());
                vars.put("expires_in", MailService.humanDuration(f.lockDuration(), account.locale()));
                vars.put("action_url", links.uiForgotPassword());
                mail.send(MailTemplate.TPL_08, account.locale(), account.emailDisplay(), account.id(), app.id(), vars);
            });
        }
    }
}
