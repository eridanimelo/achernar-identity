package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ApplicationRepository;
import com.eridani.achernar.identity.session.RefreshTokenRepository;
import com.eridani.achernar.identity.session.Session;
import com.eridani.achernar.identity.session.SessionRepository;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.crypto.TokenGenerator;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.web.Ips;
import com.eridani.achernar.identity.token.JwtIssuer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** UC-021 — renovação com rotação, tolerância de reuso e detecção de roubo (token-lifecycle §2.1). */
@Service
public class RefreshService {

    private sealed interface Outcome permits Success, Failure {
    }

    private record Success(TokenResponses.Result result) implements Outcome {
    }

    private record Failure() implements Outcome {
    }

    private final RefreshTokenRepository refreshTokens;
    private final SessionRepository sessionRepo;
    private final SessionService sessions;
    private final AccountRepository accounts;
    private final ApplicationRepository apps;
    private final AudiencePolicy audiencePolicy;
    private final JwtIssuer jwt;
    private final TokenResponses responses;
    private final RateLimiter limiter;
    private final SecurityEventRecorder events;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public RefreshService(RefreshTokenRepository refreshTokens, SessionRepository sessionRepo,
                          SessionService sessions, AccountRepository accounts, ApplicationRepository apps,
                          AudiencePolicy audiencePolicy, JwtIssuer jwt, TokenResponses responses,
                          RateLimiter limiter, SecurityEventRecorder events, AchernarProperties props,
                          Clock clock, TransactionTemplate tx) {
        this.refreshTokens = refreshTokens;
        this.sessionRepo = sessionRepo;
        this.sessions = sessions;
        this.accounts = accounts;
        this.apps = apps;
        this.audiencePolicy = audiencePolicy;
        this.jwt = jwt;
        this.responses = responses;
        this.limiter = limiter;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    /** @throws ApiException {@code INVALID_REFRESH_TOKEN} (o controlador limpa o cookie no BROWSER). */
    public TokenResponses.Result refresh(Application app, String presented, List<String> requestedAudiences) {
        limiter.check(RateRule.REFRESH_IP, Ips.subject());
        if (presented == null || presented.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        AudiencePolicy.sanitize(requestedAudiences);
        String hash = Sha256.hex(presented);
        Outcome outcome = tx.execute(status -> doRefresh(app, hash, requestedAudiences));
        if (outcome instanceof Success s) {
            return s.result();
        }
        throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private Outcome doRefresh(Application app, String hash, List<String> requested) {
        Instant now = clock.instant();
        RefreshTokenRepository.Row row = refreshTokens.findByHashForUpdate(hash).orElse(null);
        if (row == null) {
            return new Failure();
        }
        Session session = sessionRepo.findForUpdate(row.sessionId()).orElse(null);
        if (session == null || !session.applicationId().equals(app.id())) {
            return new Failure(); // RN-TOK-08
        }
        limiter.check(RateRule.REFRESH_SESSION, session.id().toString());
        if (!session.activeAt(now) || !row.expiresAt().isAfter(now) || !app.active()) {
            return new Failure();
        }
        Account account = accounts.findById(session.accountId()).orElse(null);
        if (account == null || !account.active() || account.anonymized()) {
            return new Failure();
        }
        List<String> audiences = audiencePolicy.forRefresh(app, sessionRepo.audiences(session.id()), requested);

        if (row.current()) {
            String fresh = TokenGenerator.newToken();
            UUID freshId = UUID.randomUUID();
            Instant expires = sessions.refreshExpiry(now, session.expiresAt());
            refreshTokens.markRotated(row.id(), now);
            refreshTokens.insert(freshId, session.id(), Sha256.hex(fresh), now, expires);
            refreshTokens.setReplacedBy(row.id(), freshId);
            sessionRepo.touch(session.id(), now);
            JwtIssuer.Issued access = issue(app, account, session, audiences);
            return new Success(responses.build(app, access, session.id(), audiences, fresh, expires, now, true));
        }

        UUID currentId = refreshTokens.currentIdOf(session.id()).orElse(null);
        RefreshDecision.Reuse decision = RefreshDecision.decide(row.replacedById(), currentId, row.rotatedAt(), now,
                props.refresh().reuseGrace());
        if (decision == RefreshDecision.Reuse.TOLERATED) {
            events.recordIndependent(AuditEvent.of(EventType.REFRESH_REUSE_TOLERATED).account(account.id())
                    .client(app.clientId()).session(session.id()));
            sessionRepo.touch(session.id(), now);
            JwtIssuer.Issued access = issue(app, account, session, audiences);
            return new Success(responses.build(app, access, session.id(), audiences, null, null, now, false));
        }
        sessions.revoke(session, "REUSE_DETECTED");
        events.record(AuditEvent.of(EventType.REFRESH_REUSE_DETECTED).account(account.id()).client(app.clientId())
                .session(session.id()));
        return new Failure(); // commit da revogação; erro lançado após a transação
    }

    private JwtIssuer.Issued issue(Application app, Account account, Session session, List<String> audiences) {
        return jwt.issue(new JwtIssuer.Request(account.id(), session.id(), app.clientId(), audiences, session.amr(),
                session.authTime(), account.admin(), app.accessTokenTtlSeconds()));
    }
}
