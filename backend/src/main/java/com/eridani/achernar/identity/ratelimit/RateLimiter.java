package com.eridani.achernar.identity.ratelimit;

import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.shared.error.ApiException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limitação de taxa por janela fixa em PostgreSQL (D-16, sem Redis). Cada operação usa transação própria para que
 * tentativas rejeitadas continuem contadas mesmo se a transação do chamador sofrer rollback.
 */
@Service
public class RateLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds, int count) {
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecurityEventRecorder events;
    private final TransactionTemplate independentTx;

    public RateLimiter(JdbcTemplate jdbc, Clock clock, SecurityEventRecorder events,
                       PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.events = events;
        this.independentTx = new TransactionTemplate(transactionManager);
        this.independentTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Incrementa e informa se ainda está dentro do limite (uso silencioso — respostas neutras). */
    public Decision hit(RateRule rule, String subject) {
        return independentTx.execute(status -> doHit(rule, subject));
    }

    private Decision doHit(RateRule rule, String subject) {
        Instant now = clock.instant();
        Instant windowStart = windowStart(now, rule.window());
        Integer count = jdbc.queryForObject(
                "INSERT INTO rate_limit_counter (key, window_start, count) VALUES (?, ?, 1) "
                        + "ON CONFLICT (key, window_start) DO UPDATE SET count = rate_limit_counter.count + 1 "
                        + "RETURNING count",
                Integer.class, rule.key(subject), Timestamp.from(windowStart));
        int c = count == null ? 1 : count;
        long retry = Math.max(1, Duration.between(now, windowStart.plus(rule.window())).toSeconds() + 1);
        return new Decision(c <= rule.limit(), retry, c);
    }

    /** Incrementa e lança {@code 429 RATE_LIMITED} ao exceder; registra {@code RATE_LIMIT_EXCEEDED} (1 por janela). */
    public void check(RateRule rule, String subject) {
        Decision d = hit(rule, subject);
        if (!d.allowed()) {
            if (d.count() == rule.limit() + 1) {
                events.recordIndependent(EventType.RATE_LIMIT_EXCEEDED, null, null, Map.of("rule", rule.name()));
            }
            throw ApiException.rateLimited(d.retryAfterSeconds());
        }
    }

    /** Verifica várias regras ao mesmo tempo (ex.: minuto + hora). */
    public void checkAll(String subject, List<RateRule> rules) {
        for (RateRule r : rules) {
            check(r, subject);
        }
    }

    /** Apenas consulta (sem incrementar): usado quando só falhas contam (setup, P-38). */
    public Decision peek(RateRule rule, String subject) {
        return independentTx.execute(status -> doPeek(rule, subject));
    }

    private Decision doPeek(RateRule rule, String subject) {
        Instant now = clock.instant();
        Instant windowStart = windowStart(now, rule.window());
        List<Integer> rows = jdbc.queryForList(
                "SELECT count FROM rate_limit_counter WHERE key = ? AND window_start = ?",
                Integer.class, rule.key(subject), Timestamp.from(windowStart));
        int c = rows.isEmpty() ? 0 : rows.get(0);
        long retry = Math.max(1, Duration.between(now, windowStart.plus(rule.window())).toSeconds() + 1);
        return new Decision(c < rule.limit(), retry, c);
    }

    public void requireNotExceeded(RateRule rule, String subject) {
        Decision d = peek(rule, subject);
        if (!d.allowed()) {
            throw ApiException.rateLimited(d.retryAfterSeconds());
        }
    }

    /** Remove contadores ociosos (retenção P-56). */
    @Transactional
    public int purgeOlderThan(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM rate_limit_counter WHERE ctid IN "
                + "(SELECT ctid FROM rate_limit_counter WHERE window_start < ? LIMIT ?)", Timestamp.from(cutoff), batch);
    }

    static Instant windowStart(Instant now, Duration window) {
        long w = window.toSeconds();
        return Instant.ofEpochSecond((now.getEpochSecond() / w) * w);
    }
}
