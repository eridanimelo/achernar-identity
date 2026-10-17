package com.eridani.achernar.identity.session;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.crypto.TokenGenerator;
import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.web.IpMasker;
import com.eridani.achernar.identity.shared.web.RequestInfo;
import com.eridani.achernar.identity.shared.web.RequestInfoHolder;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Criação, limite, revogação e reautenticação de sessões (RN-SES-*, D-44, RN-TOK-10). */
@Service
public class SessionService {

    public record Created(Session session, List<String> audiences, String refreshToken, Instant refreshExpiresAt) {
    }

    private final SessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final AccountRepository accounts;
    private final SecurityEventRecorder events;
    private final MailService mail;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AchernarProperties props;

    public SessionService(SessionRepository sessions, RefreshTokenRepository refreshTokens, AccountRepository accounts,
                          SecurityEventRecorder events, MailService mail, JdbcTemplate jdbc, Clock clock,
                          AchernarProperties props) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.accounts = accounts;
        this.events = events;
        this.mail = mail;
        this.jdbc = jdbc;
        this.clock = clock;
        this.props = props;
    }

    /** Cria a sessão e o primeiro refresh token; aplica o limite P-05 sob trava da conta (D-44). */
    @Transactional(propagation = Propagation.REQUIRED)
    public Created create(UUID accountId, Application app, String amr, List<String> audiences) {
        Instant now = clock.instant();
        Db.advisoryXactLock(jdbc, "account-sessions:" + accountId);
        int guard = 0;
        while (sessions.countActive(accountId, now) >= props.session().maxActive() && guard++ < 100) {
            UUID oldest = sessions.oldestActive(accountId, now).orElse(null);
            if (oldest == null) {
                break;
            }
            if (sessions.revoke(oldest, "SESSION_LIMIT", now)) {
                events.record(AuditEvent.of(EventType.SESSION_REVOKED).account(accountId).session(oldest)
                        .detail("reason", "SESSION_LIMIT"));
            }
        }
        RequestInfo info = RequestInfoHolder.get();
        String ua = info.userAgent();
        Session s = new Session(UUID.randomUUID(), accountId, app.id(), amr, now, now, now,
                now.plus(props.session().maxLifetime()), now, null, null, IpMasker.approximate(info.ip()),
                ua == null ? null : (ua.length() > 255 ? ua.substring(0, 255) : ua));
        sessions.insert(s, audiences);
        String refresh = TokenGenerator.newToken();
        Instant refreshExpires = refreshExpiry(now, s.expiresAt());
        refreshTokens.insert(UUID.randomUUID(), s.id(), Sha256.hex(refresh), now, refreshExpires);
        return new Created(s, audiences, refresh, refreshExpires);
    }

    /** Validade do refresh = mín(agora + P-02, teto absoluto da sessão) (RN-TOK-05). */
    public Instant refreshExpiry(Instant now, Instant sessionExpiresAt) {
        Instant sliding = now.plus(props.session().refreshTtl());
        return sliding.isBefore(sessionExpiresAt) ? sliding : sessionExpiresAt;
    }

    @Transactional(readOnly = true)
    public List<SessionRepository.View> list(UUID accountId) {
        return sessions.listActive(accountId, clock.instant());
    }

    /** Revoga uma sessão da própria conta (idempotente); outra conta/inexistente ⇒ NOT_FOUND (RN-SES-04). */
    @Transactional
    public void revokeOwn(UUID accountId, UUID sessionId) {
        Session s = sessions.find(sessionId).orElse(null);
        if (s == null || !s.accountId().equals(accountId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        revoke(s, "USER");
    }

    /** Revoga a sessão informada, registrando o evento (uso interno dos fluxos). */
    @Transactional
    public boolean revoke(Session s, String reason) {
        boolean changed = sessions.revoke(s.id(), reason, clock.instant());
        if (changed) {
            events.record(AuditEvent.of(EventType.SESSION_REVOKED).account(s.accountId()).session(s.id())
                    .detail("reason", reason));
        }
        return changed;
    }

    /** Revoga sessão por id (logout/reuso); retorna se houve alteração. */
    @Transactional
    public boolean revokeById(UUID sessionId, String reason) {
        Session s = sessions.find(sessionId).orElse(null);
        return s != null && revoke(s, reason);
    }

    /** Revoga todas as sessões da conta (exceto uma opcional); retorna a quantidade. */
    @Transactional
    public int revokeAllForAccount(UUID accountId, String reason, UUID exceptSessionId) {
        return sessions.revokeAllForAccount(accountId, reason, clock.instant(), exceptSessionId).size();
    }

    @Transactional
    public int revokeAllForApplication(UUID applicationId, String reason) {
        return sessions.revokeAllForApplication(applicationId, reason, clock.instant());
    }

    /** Revoga todas as sessões da instalação (remoção emergencial de chave com {@code revoke_all_sessions}). */
    @Transactional
    public int revokeEverySession(String reason) {
        return sessions.revokeAll(reason, clock.instant());
    }

    /** UC-042: encerrar todas (exige reautenticação recente). */
    @Transactional
    public int endAll(UUID accountId, UUID currentSessionId, boolean keepCurrent) {
        requireRecentReauth(currentSessionId);
        int count = revokeAllForAccount(accountId, "USER_ALL", keepCurrent ? currentSessionId : null);
        events.record(AuditEvent.of(EventType.ALL_SESSIONS_REVOKED).account(accountId).session(currentSessionId)
                .detail("count", count).detail("include_current", !keepCurrent));
        Account a = accounts.findById(accountId).orElse(null);
        if (a != null && count > 0) {
            Map<String, String> vars = new HashMap<>();
            mail.notice(MailTemplate.TPL_11, a.locale(), a.emailDisplay(), a.id(), clock.instant(), vars);
        }
        return count;
    }

    // ------------------------------------------------------------------ reautenticação (D-22)
    @Transactional(readOnly = true)
    public void requireRecentReauth(UUID sessionId) {
        Session s = sessions.find(sessionId).orElseThrow(() -> new ApiException(ErrorCode.SESSION_REVOKED));
        Instant limit = clock.instant().minus(props.session().reauthWindow());
        if (s.lastReauthAt() == null || s.lastReauthAt().isBefore(limit)) {
            throw new ApiException(ErrorCode.REAUTH_REQUIRED);
        }
    }

    @Transactional
    public Instant markReauthenticated(UUID sessionId) {
        Instant now = clock.instant();
        sessions.markReauth(sessionId, now);
        return now.plus(props.session().reauthWindow());
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId) {
        return sessions.find(sessionId).map(s -> s.activeAt(clock.instant())).orElse(false);
    }

    @Transactional(readOnly = true)
    public Session require(UUID sessionId) {
        return sessions.find(sessionId).orElseThrow(() -> new ApiException(ErrorCode.SESSION_REVOKED));
    }
}
