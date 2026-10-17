package com.eridani.achernar.identity.mail;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Processo em segundo plano do outbox (UC-095). A API nunca espera o SMTP. */
@Component
public class OutboxWorker {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxWorker.class);
    static final Duration LEASE = Duration.ofMinutes(2);

    private final MailOutbox outbox;
    private final MailRenderer renderer;
    private final SmtpMailSender sender;
    private final SecurityEventRecorder events;
    private final AchernarProperties props;
    private final Counter failedTotal;
    private final Counter sentTotal;

    public OutboxWorker(MailOutbox outbox, MailRenderer renderer, SmtpMailSender sender,
                        SecurityEventRecorder events, AchernarProperties props, MeterRegistry meters) {
        this.outbox = outbox;
        this.renderer = renderer;
        this.sender = sender;
        this.events = events;
        this.props = props;
        this.failedTotal = meters.counter("achernar_mail_outbox_failed_total");
        this.sentTotal = meters.counter("achernar_mail_outbox_sent_total");
        meters.gauge("achernar_mail_outbox_pending", outbox, o -> {
            try {
                return o.pendingCount();
            } catch (RuntimeException e) {
                return Double.NaN;
            }
        });
    }

    @Scheduled(fixedDelayString = "${achernar.mail.worker-interval:PT5S}", initialDelayString = "PT5S")
    public void tick() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            LOG.warn("Falha no ciclo do outbox de e-mail: {}", e.getClass().getSimpleName());
        }
    }

    /** Processa um lote; retorna a quantidade reservada (útil em testes). */
    public int runOnce() {
        var batch = outbox.claim(props.mail().workerBatch(), LEASE);
        for (MailOutbox.Claimed m : batch) {
            process(m);
        }
        return batch.size();
    }

    private void process(MailOutbox.Claimed m) {
        try {
            Map<String, String> vars = m.payload().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
            MailRenderer.Rendered rendered = renderer.render(m.template(), m.locale(), vars);
            sender.send(m.toAddress(), rendered);
            outbox.markSent(m.id());
            sentTotal.increment();
        } catch (Exception e) {
            handleFailure(m, e);
        }
    }

    private void handleFailure(MailOutbox.Claimed m, Exception e) {
        String safe = MailFailureClassifier.safeError(e);
        int attemptsDone = m.attempts() + 1;
        boolean permanent = MailFailureClassifier.isPermanent(e);
        Optional<Duration> delay = permanent ? Optional.empty()
                : MailBackoff.nextDelay(attemptsDone, props.mail().maxAttempts());
        if (delay.isPresent()) {
            outbox.markRetry(m.id(), delay.get(), safe);
            return;
        }
        outbox.markFailed(m.id(), safe);
        failedTotal.increment();
        events.recordIndependent(AuditEvent.of(EventType.EMAIL_DELIVERY_FAILED)
                .detail("template", m.template().id()));
        LOG.warn("E-mail {} falhou definitivamente ({})", m.template().id(), safe);
    }
}
