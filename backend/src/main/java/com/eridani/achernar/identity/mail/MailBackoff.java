package com.eridani.achernar.identity.mail;

import java.time.Duration;
import java.util.Optional;

/** Política de reentrega (P-40): 1 min, 5 min, 15 min, 1 h, 6 h; 6 tentativas no total. */
public final class MailBackoff {

    private static final Duration[] DELAYS = {
        Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(6)
    };

    private MailBackoff() {
    }

    /**
     * @param attemptsDone tentativas já realizadas (incluindo a que acabou de falhar)
     * @param maxAttempts  total de tentativas permitido
     * @return atraso até a próxima tentativa, ou vazio quando esgotado
     */
    public static Optional<Duration> nextDelay(int attemptsDone, int maxAttempts) {
        if (attemptsDone >= maxAttempts || attemptsDone < 1) {
            return Optional.empty();
        }
        int idx = Math.min(attemptsDone - 1, DELAYS.length - 1);
        return Optional.of(DELAYS[idx]);
    }
}
