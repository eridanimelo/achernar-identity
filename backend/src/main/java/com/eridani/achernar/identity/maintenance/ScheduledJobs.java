package com.eridani.achernar.identity.maintenance;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.db.JobLock;
import com.eridani.achernar.identity.ratelimit.LoginLockService;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Jobs diários (UC-091 retenção, UC-092 manutenção) com exclusão entre instâncias. */
@Component
public class ScheduledJobs {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobs.class);

    private final RetentionService retention;
    private final RateLimiter limiter;
    private final LoginLockService locks;
    private final JobLock jobLock;
    private final AchernarProperties props;
    private final Clock clock;

    public ScheduledJobs(RetentionService retention, RateLimiter limiter, LoginLockService locks, JobLock jobLock,
                         AchernarProperties props, Clock clock) {
        this.retention = retention;
        this.limiter = limiter;
        this.locks = locks;
        this.jobLock = jobLock;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(cron = "${achernar.jobs.retention-cron}", zone = "UTC")
    public void retention() {
        boolean ran = jobLock.runExclusive("job-retention", () -> LOG.info("Retenção concluída: {}", retention.run()));
        if (!ran) {
            LOG.debug("Retenção em execução em outra instância");
        }
    }

    @Scheduled(cron = "${achernar.jobs.maintenance-cron}", zone = "UTC")
    public void maintenance() {
        jobLock.runExclusive("job-maintenance", () -> {
            int batch = props.retention().batchSize();
            Duration idle = props.retention().rateLimit();
            int counters = limiter.purgeOlderThan(clock.instant().minus(idle), batch);
            int lockRows = locks.purgeIdle(clock.instant().minus(idle), batch);
            LOG.info("Manutenção concluída: contadores={} bloqueios={}", counters, lockRows);
        });
    }
}
