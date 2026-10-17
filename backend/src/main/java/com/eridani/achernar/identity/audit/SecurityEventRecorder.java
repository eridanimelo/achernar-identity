package com.eridani.achernar.identity.audit;

import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.json.MiniJson;
import com.eridani.achernar.identity.shared.util.UuidV7;
import com.eridani.achernar.identity.shared.web.RequestInfo;
import com.eridani.achernar.identity.shared.web.RequestInfoHolder;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registra eventos de segurança (D-26). {@link #record} participa da transação corrente (mudanças de estado);
 * {@link #recordIndependent} usa transação própria para sobreviver a rollback (eventos de falha).
 */
@Service
public class SecurityEventRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityEventRecorder.class);

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final MeterRegistry meters;
    private final TransactionTemplate independentTx;

    public SecurityEventRecorder(JdbcTemplate jdbc, Clock clock, MeterRegistry meters,
                                 PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.meters = meters;
        this.independentTx = new TransactionTemplate(transactionManager);
        this.independentTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Registra na transação corrente (cria uma se não houver). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(AuditEvent event) {
        persist(event);
    }

    /** Registra em transação independente (sobrevive a rollback do chamador). */
    public void recordIndependent(AuditEvent event) {
        independentTx.executeWithoutResult(status -> persist(event));
    }

    /** Atalho para eventos simples na transação corrente. */
    public void record(EventType type, UUID accountId, Map<String, ?> details) {
        record(AuditEvent.of(type).account(accountId).details(details));
    }

    /** Atalho para eventos independentes (falhas). */
    public void recordIndependent(EventType type, UUID accountId, String clientId, Map<String, ?> details) {
        recordIndependent(AuditEvent.of(type).account(accountId).client(clientId).details(details));
    }

    private void persist(AuditEvent e) {
        Instant now = clock.instant();
        RequestInfo info = RequestInfoHolder.get();
        Map<String, Object> filtered = new LinkedHashMap<>();
        e.details().forEach((k, v) -> {
            if (e.type().allows(k)) {
                filtered.put(k, v);
            }
        });
        jdbc.update("INSERT INTO security_event (id, occurred_at, event_type, severity, outcome, account_id, "
                        + "actor_account_id, client_id, session_id, ip, user_agent, request_id, details) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UuidV7.generate(now), Db.ts(now), e.type().name(), e.type().severity().name(), e.outcome().name(),
                e.accountId(), e.actorAccountId(), e.clientId(), e.sessionId(), info.ip(), info.userAgent(),
                info.requestId(), MiniJson.write(filtered));
        if (e.type().severity() == Severity.ALERT) {
            LOG.warn("Evento de segurança ALERT type={} account_id={} client_id={} request_id={}",
                    e.type(), e.accountId(), e.clientId(), info.requestId());
            meters.counter("achernar_security_alert_total", "type", e.type().name()).increment();
        }
    }

    // ------------------------------------------------------------ retenção (UC-091)
    @Transactional
    public int purgeOlderThan(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM security_event WHERE id IN (SELECT id FROM security_event "
                + "WHERE occurred_at < ? LIMIT ?)", Db.ts(cutoff), batch);
    }

    @Transactional
    public int anonymizeNetworkData(Instant cutoff, int batch) {
        return jdbc.update("UPDATE security_event SET ip = NULL, user_agent = NULL WHERE id IN "
                + "(SELECT id FROM security_event WHERE occurred_at < ? AND (ip IS NOT NULL OR user_agent IS NOT NULL) "
                + "LIMIT ?)", Db.ts(cutoff), batch);
    }
}
