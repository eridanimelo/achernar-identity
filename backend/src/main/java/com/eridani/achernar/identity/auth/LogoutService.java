package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.session.RefreshTokenRepository;
import com.eridani.achernar.identity.session.Session;
import com.eridani.achernar.identity.session.SessionRepository;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.web.Ips;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** UC-022 — encerra a sessão do refresh apresentado; idempotente (RN-AUT-09). */
@Service
public class LogoutService {

    private final RefreshTokenRepository refreshTokens;
    private final SessionRepository sessionRepo;
    private final RateLimiter limiter;
    private final SecurityEventRecorder events;
    private final Clock clock;
    private final TransactionTemplate tx;

    public LogoutService(RefreshTokenRepository refreshTokens, SessionRepository sessionRepo, RateLimiter limiter,
                         SecurityEventRecorder events, Clock clock, TransactionTemplate tx) {
        this.refreshTokens = refreshTokens;
        this.sessionRepo = sessionRepo;
        this.limiter = limiter;
        this.events = events;
        this.clock = clock;
        this.tx = tx;
    }

    public void logout(Application app, String presented) {
        limiter.check(RateRule.REFRESH_IP, Ips.subject());
        if (presented == null || presented.isBlank()) {
            return;
        }
        String hash = Sha256.hex(presented);
        tx.executeWithoutResult(status -> {
            RefreshTokenRepository.Row row = refreshTokens.findByHashForUpdate(hash).orElse(null);
            if (row == null) {
                return;
            }
            Session s = sessionRepo.findForUpdate(row.sessionId()).orElse(null);
            if (s == null || !s.applicationId().equals(app.id())) {
                return;
            }
            if (sessionRepo.revoke(s.id(), "LOGOUT", clock.instant())) {
                events.record(AuditEvent.of(EventType.LOGOUT).account(s.accountId()).client(app.clientId())
                        .session(s.id()));
                events.record(AuditEvent.of(EventType.SESSION_REVOKED).account(s.accountId()).session(s.id())
                        .detail("reason", "LOGOUT"));
            }
        });
    }
}
