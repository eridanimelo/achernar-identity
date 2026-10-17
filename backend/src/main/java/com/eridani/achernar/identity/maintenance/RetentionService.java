package com.eridani.achernar.identity.maintenance;

import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.account.OneTimeTokenService;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.mail.MailOutbox;
import com.eridani.achernar.identity.ratelimit.LoginLockService;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.session.SessionRepository;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.social.SocialExchangeCodeRepository;
import com.eridani.achernar.identity.social.SocialTransactionRepository;
import com.eridani.achernar.identity.token.SigningKeyService;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** UC-091 — retenção de dados (RN-RET-*): lotes idempotentes, falha parcial não interrompe os demais alvos. */
@Service
public class RetentionService {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionService.class);
    private static final int MAX_BATCHES = 10_000;

    private final SecurityEventRecorder events;
    private final AccountRepository accounts;
    private final SessionRepository sessions;
    private final OneTimeTokenService tokens;
    private final SocialTransactionRepository socialTx;
    private final SocialExchangeCodeRepository socialCodes;
    private final MailOutbox outbox;
    private final RateLimiter limiter;
    private final LoginLockService locks;
    private final SigningKeyService keys;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public RetentionService(SecurityEventRecorder events, AccountRepository accounts, SessionRepository sessions,
                            OneTimeTokenService tokens, SocialTransactionRepository socialTx,
                            SocialExchangeCodeRepository socialCodes, MailOutbox outbox, RateLimiter limiter,
                            LoginLockService locks, SigningKeyService keys, AchernarProperties props, Clock clock,
                            TransactionTemplate tx) {
        this.events = events;
        this.accounts = accounts;
        this.sessions = sessions;
        this.tokens = tokens;
        this.socialTx = socialTx;
        this.socialCodes = socialCodes;
        this.outbox = outbox;
        this.limiter = limiter;
        this.locks = locks;
        this.keys = keys;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    /** Executa todos os alvos e registra {@code RETENTION_PURGE_RUN}; devolve as contagens. */
    public Map<String, Object> run() {
        Instant now = clock.instant();
        AchernarProperties.Retention r = props.retention();
        int batch = r.batchSize();
        Map<String, Object> counts = new LinkedHashMap<>();

        step(counts, "pending_accounts", () -> drain(batch, () -> {
            List<java.util.UUID> ids = accounts.pendingOlderThan(now.minus(r.unverifiedAccount()), batch);
            int n = 0;
            for (java.util.UUID id : ids) {
                n += tx.execute(s -> accounts.deletePending(id));
            }
            return n;
        }));
        step(counts, "sessions", () -> drain(batch, () -> tx.execute(s ->
                sessions.purge(now.minus(r.sessions()), batch))));
        step(counts, "one_time_tokens", () -> drain(batch, () -> tx.execute(s ->
                tokens.purge(now.minus(r.oneTimeTokens()), batch))));
        step(counts, "social_transactions", () -> drain(batch, () -> tx.execute(s ->
                socialTx.purgeOlderThan(now.minus(r.oneTimeTokens()), batch))));
        step(counts, "social_codes", () -> drain(batch, () -> tx.execute(s ->
                socialCodes.purgeOlderThan(now.minus(r.oneTimeTokens()), batch))));
        step(counts, "email_outbox", () -> drain(batch, () -> tx.execute(s ->
                outbox.purge(now.minus(r.outboxSent()), now.minus(props.mail().failedRetention()), batch))));
        step(counts, "rate_limit", () -> drain(batch, () -> tx.execute(s ->
                limiter.purgeOlderThan(now.minus(r.rateLimit()), batch))));
        step(counts, "login_lock", () -> drain(batch, () -> tx.execute(s ->
                locks.purgeIdle(now.minus(r.rateLimit()), batch))));
        step(counts, "signing_keys", () -> drain(batch, () -> tx.execute(s ->
                keys.purgeRemoved(now.minus(r.retiredKeys()), batch))));
        step(counts, "event_network_data", () -> drain(batch, () -> tx.execute(s ->
                events.anonymizeNetworkData(now.minus(r.eventIp()), batch))));
        step(counts, "security_events", () -> drain(batch, () -> tx.execute(s ->
                events.purgeOlderThan(now.minus(r.securityEvents()), batch))));

        tx.executeWithoutResult(s -> events.record(AuditEvent.of(EventType.RETENTION_PURGE_RUN)
                .detail("counts", counts)));
        return counts;
    }

    private void step(Map<String, Object> counts, String target, IntSupplier action) {
        try {
            counts.put(target, action.getAsInt());
        } catch (RuntimeException e) {
            LOG.error("Falha parcial na retenção do alvo {} (a próxima execução continua)", target, e);
            counts.put(target, -1);
        }
    }

    /** Repete o lote até esgotar (lote incompleto) ou atingir o teto de segurança. */
    static int drain(int batch, IntSupplier step) {
        int total = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            int n = step.getAsInt();
            total += n;
            if (n < batch) {
                break;
            }
        }
        return total;
    }
}
