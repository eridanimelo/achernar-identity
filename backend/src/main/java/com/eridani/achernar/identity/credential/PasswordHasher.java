package com.eridani.achernar.identity.credential;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Argon2id (D-15, SEC-02) com semáforo de concorrência (SEC-06): fila com timeout de 2 s ⇒ {@code 503}.
 * {@link #verifyDummy} equaliza o tempo quando a conta não existe (D-48).
 */
@Component
public class PasswordHasher {

    private static final Pattern PARAMS = Pattern.compile("\\$argon2id\\$v=\\d+\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$");
    private static final long WAIT_SECONDS = 2;

    private final Argon2PasswordEncoder encoder;
    private final int memoryKib;
    private final int iterations;
    private final int parallelism;
    private final Semaphore permits;
    private final Timer waitTimer;
    private volatile String dummyHash;

    public PasswordHasher(AchernarProperties props, MeterRegistry meters) {
        AchernarProperties.Password p = props.password();
        this.memoryKib = p.argon2MemoryKib();
        this.iterations = p.argon2Iterations();
        this.parallelism = p.argon2Parallelism();
        this.encoder = new Argon2PasswordEncoder(16, 32, parallelism, memoryKib, iterations);
        int concurrency = p.hashConcurrency() > 0 ? p.hashConcurrency()
                : Math.max(2, 2 * Runtime.getRuntime().availableProcessors());
        this.permits = new Semaphore(concurrency, true);
        this.waitTimer = Timer.builder("achernar_hash_queue_wait_seconds").register(meters);
    }

    public String hash(String password) {
        return withPermit(() -> encoder.encode(PasswordPolicy.normalize(password)));
    }

    public boolean verify(String password, String storedHash) {
        return withPermit(() -> encoder.matches(PasswordPolicy.normalize(password), storedHash));
    }

    /** Verificação contra hash fictício de mesma configuração; sempre falso (D-48). */
    public boolean verifyDummy(String password) {
        String dummy = dummyHash;
        if (dummy == null) {
            dummy = hash("achernar-dummy-password-for-timing-equalization");
            dummyHash = dummy;
        }
        verify(password, dummy);
        return false;
    }

    /** Hash gravado com parâmetros diferentes dos atuais (RN-AUT-07)? */
    public boolean needsRehash(String storedHash) {
        Matcher m = PARAMS.matcher(storedHash == null ? "" : storedHash);
        if (!m.find()) {
            return true;
        }
        return Integer.parseInt(m.group(1)) != memoryKib || Integer.parseInt(m.group(2)) != iterations
                || Integer.parseInt(m.group(3)) != parallelism;
    }

    private <T> T withPermit(java.util.function.Supplier<T> action) {
        long start = System.nanoTime();
        boolean acquired;
        try {
            acquired = permits.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, null, List.of(), WAIT_SECONDS, e);
        }
        waitTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        if (!acquired) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, null, List.of(), WAIT_SECONDS, null);
        }
        try {
            return action.get();
        } finally {
            permits.release();
        }
    }
}
