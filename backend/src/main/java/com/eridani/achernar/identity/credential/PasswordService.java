package com.eridani.achernar.identity.credential;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.account.AccountStatus;
import com.eridani.achernar.identity.account.OneTimeTokenService;
import com.eridani.achernar.identity.account.RegistrationService;
import com.eridani.achernar.identity.account.TokenPurpose;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.mail.ActionLinks;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.ratelimit.LoginLockService;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.FieldError;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.Ips;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** FT-04: recuperação (UC-030/031), alteração/definição (UC-032/037) e reautenticação (UC-033). */
@Service
public class PasswordService {

    static final int MAX_INPUT = 1024;

    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final PasswordPolicy policy;
    private final PasswordHasher hasher;
    private final OneTimeTokenService tokens;
    private final RegistrationService registration;
    private final SessionService sessions;
    private final LoginLockService locks;
    private final RateLimiter limiter;
    private final MailService mail;
    private final ActionLinks links;
    private final SecurityEventRecorder events;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public PasswordService(AccountRepository accounts, PasswordCredentialRepository credentials,
                           PasswordPolicy policy, PasswordHasher hasher, OneTimeTokenService tokens,
                           RegistrationService registration, SessionService sessions, LoginLockService locks,
                           RateLimiter limiter, MailService mail, ActionLinks links, SecurityEventRecorder events,
                           AchernarProperties props, Clock clock, TransactionTemplate tx) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.policy = policy;
        this.hasher = hasher;
        this.tokens = tokens;
        this.registration = registration;
        this.sessions = sessions;
        this.locks = locks;
        this.limiter = limiter;
        this.mail = mail;
        this.links = links;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    // ------------------------------------------------------------------ UC-030
    public void forgot(ClientContext client, String email) {
        EmailAddress.Normalized parsed = EmailAddress.parse(email)
                .orElseThrow(() -> ApiException.validation("email", "INVALID_FORMAT"));
        limiter.check(RateRule.FORGOT_IP, Ips.subject());
        if (!limiter.hit(RateRule.FORGOT_EMAIL, parsed.normalized()).allowed()) {
            return; // 202 silencioso (P-33)
        }
        Application app = client.application();
        tx.executeWithoutResult(status -> {
            Account a = accounts.findByEmail(parsed.normalized()).filter(x -> !x.anonymized()).orElse(null);
            if (a == null) {
                return;
            }
            if (a.status() == AccountStatus.PENDING_VERIFICATION) {
                registration.sendVerification(a, app);
                return;
            }
            if (a.status() != AccountStatus.ACTIVE) {
                return;
            }
            Duration ttl = props.oneTime().passwordResetTtl();
            OneTimeTokenService.Issued issued = tokens.issue(a.id(), TokenPurpose.PASSWORD_RESET, app.id(), null,
                    null, ttl);
            Map<String, String> vars = new HashMap<>();
            vars.put("app_name", app.name());
            vars.put("action_url", links.resetPassword(app.resetPasswordUrl(), issued.token()));
            vars.put("expires_in", MailService.humanDuration(ttl, a.locale()));
            mail.send(MailTemplate.TPL_02, a.locale(), a.emailDisplay(), a.id(), app.id(), vars);
            events.record(AuditEvent.of(EventType.PASSWORD_RESET_REQUESTED).account(a.id())
                    .client(app.clientId()));
        });
    }

    // ------------------------------------------------------------------ UC-031
    public void reset(ClientContext client, String token, String newPassword) {
        limiter.checkAll(Ips.subject(), List.of(RateRule.AUTH_IP_MINUTE, RateRule.AUTH_IP_HOUR));
        if (token == null || token.isBlank()) {
            throw ApiException.validation("token", "REQUIRED");
        }
        requireInput("new_password", newPassword);
        tx.executeWithoutResult(status -> {
            OneTimeTokenService.Consumed c = tokens.consume(token, TokenPurpose.PASSWORD_RESET)
                    .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED));
            Account a = accounts.findByIdForUpdate(c.accountId()).orElse(null);
            if (a == null || a.status() != AccountStatus.ACTIVE || a.anonymized()) {
                throw new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED);
            }
            checkPolicy(newPassword, a.emailNormalized()); // falha ⇒ rollback ⇒ token não consumido (CA-FT04-07)
            Instant now = clock.instant();
            credentials.upsert(a.id(), hasher.hash(newPassword), now);
            sessions.revokeAllForAccount(a.id(), "PASSWORD_RESET", null);
            locks.reset(a.emailNormalized());
            tokens.invalidate(a.id(), TokenPurpose.PASSWORD_RESET, TokenPurpose.EMAIL_CHANGE);
            notifyChanged(a, now);
            events.record(AuditEvent.of(EventType.PASSWORD_RESET_COMPLETED).account(a.id())
                    .client(client.application().clientId()));
        });
    }

    // ------------------------------------------------------------------ UC-032 / UC-037
    public void change(AccessPrincipal who, String currentPassword, String newPassword) {
        requireInput("new_password", newPassword);
        if (currentPassword != null && currentPassword.length() > MAX_INPUT) {
            throw ApiException.validation("current_password", "TOO_LONG");
        }
        if (currentPassword != null && !currentPassword.isEmpty()) {
            limiter.check(RateRule.REAUTH_SESSION, who.sessionId().toString());
        }
        tx.executeWithoutResult(status -> {
            Account a = accounts.findByIdForUpdate(who.accountId())
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
            Optional<PasswordCredentialRepository.Credential> existing = credentials.find(a.id());
            boolean hasPassword = existing.isPresent();
            if (hasPassword && currentPassword != null && !currentPassword.isEmpty()) {
                if (!verifyCurrent(a, who, existing.get().hash(), currentPassword)) {
                    // transação marcada para rollback pela exceção; eventos independentes já gravados
                    throw new ApiException(ErrorCode.REAUTH_FAILED);
                }
            } else {
                sessions.requireRecentReauth(who.sessionId());
            }
            if (hasPassword && hasher.verify(newPassword, existing.get().hash())) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, List.of(FieldError.of("new_password",
                        "SAME_AS_CURRENT")));
            }
            checkPolicy(newPassword, a.emailNormalized());
            Instant now = clock.instant();
            credentials.upsert(a.id(), hasher.hash(newPassword), now);
            if (hasPassword) {
                sessions.revokeAllForAccount(a.id(), "PASSWORD_CHANGED", who.sessionId());
            }
            tokens.invalidate(a.id(), TokenPurpose.PASSWORD_RESET);
            notifyChanged(a, now);
            events.record(AuditEvent.of(hasPassword ? EventType.PASSWORD_CHANGED : EventType.PASSWORD_SET)
                    .account(a.id()).client(who.clientId()).session(who.sessionId()));
        });
    }

    // ------------------------------------------------------------------ UC-033
    /** @return instante até o qual a reautenticação vale */
    public Instant reauthenticate(AccessPrincipal who, String password) {
        requireInput("password", password);
        limiter.check(RateRule.REAUTH_SESSION, who.sessionId().toString());
        Account a = accounts.findById(who.accountId()).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        PasswordCredentialRepository.Credential cred = credentials.find(a.id()).orElse(null);
        if (cred == null) {
            throw ApiException.validation("password", "NO_PASSWORD");
        }
        if (!verifyCurrent(a, who, cred.hash(), password)) {
            throw new ApiException(ErrorCode.REAUTH_FAILED);
        }
        return tx.execute(status -> {
            Instant until = sessions.markReauthenticated(who.sessionId());
            events.record(AuditEvent.of(EventType.REAUTH_SUCCEEDED).account(a.id()).client(who.clientId())
                    .session(who.sessionId()).detail("method", "password"));
            return until;
        });
    }

    /** Verifica a senha atual; falha conta no bloqueio de login (RN-PWD-08) e registra REAUTH_FAILED. */
    private boolean verifyCurrent(Account a, AccessPrincipal who, String hash, String candidate) {
        boolean locked = locks.remainingLockSeconds(a.emailNormalized()) > 0;
        boolean ok = false;
        if (locked) {
            hasher.verifyDummy(candidate); // tempo equivalente; bloqueado nunca autentica
        } else {
            ok = hasher.verify(candidate, hash);
        }
        if (ok) {
            return true;
        }
        LoginLockService.Failure f = locks.registerFailure(a.emailNormalized());
        events.recordIndependent(EventType.REAUTH_FAILED, a.id(), who.clientId(), Map.of("method", "password"));
        if (f.newlyLocked()) {
            events.recordIndependent(EventType.ACCOUNT_LOCKED_THROTTLE, a.id(), who.clientId(),
                    Map.of("lock_seconds", f.lockDuration().toSeconds()));
        }
        return false;
    }

    private void checkPolicy(String password, String emailNormalized) {
        List<FieldError> violations = policy.check(password, emailNormalized);
        if (!violations.isEmpty()) {
            throw new ApiException(ErrorCode.PASSWORD_POLICY_VIOLATION,
                    violations.stream().map(v -> FieldError.of("new_password", v.code())).toList());
        }
    }

    private void notifyChanged(Account a, Instant now) {
        Map<String, String> vars = new HashMap<>();
        vars.put("action_url", links.uiForgotPassword());
        mail.notice(MailTemplate.TPL_06, a.locale(), a.emailDisplay(), a.id(), now, vars);
    }

    private static void requireInput(String field, String value) {
        if (value == null || value.isEmpty()) {
            throw ApiException.validation(field, "REQUIRED");
        }
        if (value.length() > MAX_INPUT) {
            throw ApiException.validation(field, "TOO_LONG");
        }
    }
}
