package com.eridani.achernar.identity.admin;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.account.AccountRole;
import com.eridani.achernar.identity.account.AccountStatus;
import com.eridani.achernar.identity.account.OneTimeTokenService;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.Outcome;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.credential.PasswordCredentialRepository;
import com.eridani.achernar.identity.mail.MailOutbox;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.db.PageResult;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.util.Time;
import com.eridani.achernar.identity.social.SocialLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** FT-11: UC-080…084 (consulta, desativar, reativar, perfil, anonimizar). */
@Service
public class AccountAdminService {

    static final String ADMIN_LOCK = "admin-accounts";
    static final int MAX_REASON = 200;

    private final AdminAccountRepository queries;
    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final SocialLinkRepository links;
    private final OneTimeTokenService tokens;
    private final SessionService sessions;
    private final MailService mail;
    private final MailOutbox outbox;
    private final SecurityEventRecorder events;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate tx;

    public AccountAdminService(AdminAccountRepository queries, AccountRepository accounts,
                               PasswordCredentialRepository credentials, SocialLinkRepository links,
                               OneTimeTokenService tokens, SessionService sessions, MailService mail,
                               MailOutbox outbox, SecurityEventRecorder events, JdbcTemplate jdbc, Clock clock,
                               TransactionTemplate tx) {
        this.queries = queries;
        this.accounts = accounts;
        this.credentials = credentials;
        this.links = links;
        this.tokens = tokens;
        this.sessions = sessions;
        this.mail = mail;
        this.outbox = outbox;
        this.events = events;
        this.jdbc = jdbc;
        this.clock = clock;
        this.tx = tx;
    }

    // ------------------------------------------------------------------ UC-080
    public Map<String, Object> list(AdminAccountRepository.Filter f, Integer page, Integer size, String sort) {
        if (f.q() != null && f.q().length() < 3) {
            throw ApiException.validation("q", "TOO_SHORT");
        }
        if (f.status() != null && !List.of("PENDING_VERIFICATION", "ACTIVE", "DISABLED").contains(f.status())) {
            throw ApiException.validation("status", "INVALID");
        }
        if (f.role() != null && !List.of("USER", "ADMIN").contains(f.role())) {
            throw ApiException.validation("role", "INVALID");
        }
        if (f.method() != null && !List.of("password", "google", "facebook").contains(f.method())) {
            throw ApiException.validation("method", "INVALID");
        }
        PageResult<AdminAccountRepository.Row> r = queries.list(f, PageResult.clampPage(page),
                PageResult.clampSize(size), sort);
        return r.toBody(AccountAdminService::summary);
    }

    public Map<String, Object> detail(UUID id) {
        AdminAccountRepository.Row row = queries.find(id);
        Obj o = summary(row);
        o.with("locale", row.locale()).with("disabled_at", Time.iso(row.disabledAt()))
                .with("disabled_reason", row.disabledReason()).with("anonymized_at", Time.iso(row.anonymizedAt()))
                .with("active_sessions", queries.activeSessions(id, clock.instant()))
                .with("social_links", links.findByAccount(id).stream().map(l -> Obj.of()
                        .with("provider", l.provider().slug()).with("linked_at", Time.iso(l.linkedAt()))).toList())
                .with("recent_events", queries.recentEvents(id, 20).stream().map(e -> Obj.of()
                        .with("id", e.id().toString()).with("occurred_at", Time.iso(e.occurredAt()))
                        .with("event_type", e.eventType()).with("severity", e.severity())
                        .with("outcome", e.outcome())).toList())
                .with("version", row.version());
        return o;
    }

    static Obj summary(AdminAccountRepository.Row r) {
        return Obj.of().with("id", r.id().toString()).with("email", r.email()).with("status", r.status())
                .with("role", r.role()).with("email_verified", r.emailVerified())
                .with("has_password", r.hasPassword()).with("providers", r.providers())
                .with("created_at", Time.iso(r.createdAt()));
    }

    // ------------------------------------------------------------------ UC-081
    public void disable(AccessPrincipal admin, UUID targetId, String reason) {
        if (reason != null && reason.length() > MAX_REASON) {
            throw ApiException.validation("reason", "TOO_LONG");
        }
        sessions.requireRecentReauth(admin.sessionId());
        if (admin.accountId().equals(targetId)) {
            denied(EventType.ACCOUNT_DISABLED, admin, targetId);
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        tx.executeWithoutResult(status -> {
            Db.advisoryXactLock(jdbc, ADMIN_LOCK);
            Account t = accounts.findByIdForUpdate(targetId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            if (t.status() == AccountStatus.DISABLED) {
                return; // idempotente: nenhum novo efeito (CA-FT11-19)
            }
            if (t.admin() && t.active() && accounts.countActiveAdmins() <= 1) {
                lastAdmin(EventType.ACCOUNT_DISABLED, admin, targetId);
            }
            Instant now = clock.instant();
            accounts.disable(targetId, reason, now);
            sessions.revokeAllForAccount(targetId, "ADMIN_DISABLED", null);
            tokens.invalidateAll(targetId);
            Map<String, String> vars = new HashMap<>();
            vars.put("variant", "disabled");
            mail.notice(MailTemplate.TPL_09, t.locale(), t.emailDisplay(), t.id(), now, vars);
            events.record(AuditEvent.of(EventType.ACCOUNT_DISABLED).account(targetId).actor(admin.accountId())
                    .client(admin.clientId()).session(admin.sessionId()).detail("reason", reason));
        });
    }

    // ------------------------------------------------------------------ UC-082
    public void enable(AccessPrincipal admin, UUID targetId) {
        tx.executeWithoutResult(status -> {
            Account t = accounts.findByIdForUpdate(targetId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            if (t.anonymized() || t.status() == AccountStatus.PENDING_VERIFICATION) {
                throw new ApiException(ErrorCode.CONFLICT);
            }
            if (t.status() == AccountStatus.ACTIVE) {
                return; // idempotente
            }
            Instant now = clock.instant();
            accounts.enable(targetId, now);
            Map<String, String> vars = new HashMap<>();
            vars.put("variant", "enabled");
            mail.notice(MailTemplate.TPL_09, t.locale(), t.emailDisplay(), t.id(), now, vars);
            events.record(AuditEvent.of(EventType.ACCOUNT_ENABLED).account(targetId).actor(admin.accountId())
                    .client(admin.clientId()).session(admin.sessionId()));
        });
    }

    // ------------------------------------------------------------------ UC-083
    public Map<String, Object> changeRole(AccessPrincipal admin, UUID targetId, String roleRaw, Long version) {
        AccountRole role;
        try {
            role = AccountRole.valueOf(roleRaw == null ? "" : roleRaw);
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("role", "INVALID");
        }
        if (version == null) {
            throw ApiException.validation("version", "REQUIRED");
        }
        sessions.requireRecentReauth(admin.sessionId());
        tx.executeWithoutResult(status -> {
            Db.advisoryXactLock(jdbc, ADMIN_LOCK);
            Account t = accounts.findByIdForUpdate(targetId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            if (t.version() != version) {
                throw new ApiException(ErrorCode.CONFLICT);
            }
            if (t.role() == role) {
                return;
            }
            EventType type = role == AccountRole.ADMIN ? EventType.ADMIN_GRANTED : EventType.ADMIN_REVOKED;
            if (role == AccountRole.ADMIN) {
                if (!t.active() || t.emailVerifiedAt() == null || t.anonymized()) {
                    throw new ApiException(ErrorCode.CONFLICT);
                }
            } else if (t.active() && accounts.countActiveAdmins() <= 1) {
                lastAdmin(type, admin, targetId);
            }
            if (!accounts.updateRole(targetId, role, version, clock.instant())) {
                throw new ApiException(ErrorCode.CONFLICT);
            }
            events.record(AuditEvent.of(type).account(targetId).actor(admin.accountId()).client(admin.clientId())
                    .session(admin.sessionId()));
        });
        return detail(targetId);
    }

    // ------------------------------------------------------------------ UC-084
    public void anonymize(AccessPrincipal admin, UUID targetId, String confirmEmail) {
        sessions.requireRecentReauth(admin.sessionId());
        EmailAddress.Normalized confirm = confirmEmail == null ? null : EmailAddress.parse(confirmEmail).orElse(null);
        if (confirm == null) {
            throw ApiException.validation("confirm_email", "REQUIRED");
        }
        tx.executeWithoutResult(status -> {
            Db.advisoryXactLock(jdbc, ADMIN_LOCK);
            Account t = accounts.findByIdForUpdate(targetId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            if (t.anonymized() || t.status() != AccountStatus.DISABLED) {
                throw new ApiException(ErrorCode.CONFLICT);
            }
            if (t.admin() && t.active()) {
                lastAdmin(EventType.ACCOUNT_ANONYMIZED, admin, targetId);
            }
            if (!t.emailNormalized().equals(confirm.normalized())) {
                throw ApiException.validation("confirm_email", "MISMATCH");
            }
            Instant now = clock.instant();
            String anon = "anon-" + targetId + "@anonymized.invalid";
            accounts.anonymize(targetId, anon, now);
            credentials.delete(targetId);
            links.deleteAll(targetId);
            sessions.revokeAllForAccount(targetId, "ANONYMIZED", null);
            tokens.invalidateAll(targetId);
            outbox.purgeForAccount(targetId);
            events.record(AuditEvent.of(EventType.ACCOUNT_ANONYMIZED).account(targetId).actor(admin.accountId())
                    .client(admin.clientId()).session(admin.sessionId()));
        });
    }

    // ------------------------------------------------------------------ util
    private void lastAdmin(EventType type, AccessPrincipal admin, UUID targetId) {
        denied(type, admin, targetId);
        throw new ApiException(ErrorCode.LAST_ADMIN_PROTECTED);
    }

    /** Falha por regra de negócio também é auditada (CA-FT11-16), em transação independente. */
    private void denied(EventType type, AccessPrincipal admin, UUID targetId) {
        events.recordIndependent(AuditEvent.of(type).account(targetId).actor(admin.accountId())
                .client(admin.clientId()).session(admin.sessionId()).outcome(Outcome.FAILURE));
    }
}
