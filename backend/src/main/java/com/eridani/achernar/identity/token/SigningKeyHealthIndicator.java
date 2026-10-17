package com.eridani.achernar.identity.token;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Readiness: deve existir uma chave de assinatura {@code ACTIVE} (architecture §6). */
@Component("signingKey")
public class SigningKeyHealthIndicator implements HealthIndicator {

    private final SigningKeyService keys;
    private final AtomicReference<Instant> lastCheck = new AtomicReference<>(Instant.EPOCH);
    private volatile boolean lastResult;

    public SigningKeyHealthIndicator(SigningKeyService keys) {
        this.keys = keys;
    }

    @Override
    public Health health() {
        Instant now = Instant.now();
        if (Duration.between(lastCheck.get(), now).toSeconds() >= 5) {
            lastResult = keys.hasActiveKey();
            lastCheck.set(now);
        }
        return lastResult ? Health.up().build() : Health.down().withDetail("reason", "NO_ACTIVE_SIGNING_KEY").build();
    }
}
