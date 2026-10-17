package com.eridani.achernar.identity.ratelimit;

import java.time.Duration;
import java.time.Instant;

/**
 * Política de bloqueio de login por conta (P-30, D-16): 5 falhas em 15 min ⇒ bloqueio de 15 min; reincidência em
 * 24 h dobra (30, 60, 120 min; teto 120). Lógica pura e testável.
 */
public final class LoginLockPolicy {

    public static final int THRESHOLD = 5;
    public static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    public static final Duration RECIDIVISM_WINDOW = Duration.ofHours(24);
    public static final Duration BASE_LOCK = Duration.ofMinutes(15);
    public static final Duration MAX_LOCK = Duration.ofMinutes(120);

    public record State(int failureCount, Instant windowStart, Instant lockedUntil, int lockLevel, Instant lastLockAt) {
        public static State empty(Instant now) {
            return new State(0, now, null, 0, null);
        }
    }

    public record Result(State state, boolean newlyLocked, Duration lockDuration) {
    }

    private LoginLockPolicy() {
    }

    public static boolean isLocked(State s, Instant now) {
        return s != null && s.lockedUntil() != null && s.lockedUntil().isAfter(now);
    }

    public static long remainingSeconds(State s, Instant now) {
        if (!isLocked(s, now)) {
            return 0;
        }
        return Math.max(1, Duration.between(now, s.lockedUntil()).toSeconds() + 1);
    }

    public static Result onFailure(State current, Instant now) {
        State s = current == null ? State.empty(now) : current;
        if (isLocked(s, now)) {
            return new Result(s, false, Duration.ZERO);
        }
        int count = s.failureCount();
        Instant windowStart = s.windowStart();
        if (windowStart == null || Duration.between(windowStart, now).compareTo(FAILURE_WINDOW) > 0) {
            count = 0;
            windowStart = now;
        }
        count++;
        if (count < THRESHOLD) {
            return new Result(new State(count, windowStart, s.lockedUntil(), s.lockLevel(), s.lastLockAt()), false, Duration.ZERO);
        }
        boolean recidivism = s.lastLockAt() != null
                && Duration.between(s.lastLockAt(), now).compareTo(RECIDIVISM_WINDOW) <= 0;
        int level = recidivism ? s.lockLevel() + 1 : 1;
        Duration duration = durationForLevel(level);
        return new Result(new State(0, now, now.plus(duration), level, now), true, duration);
    }

    public static Duration durationForLevel(int level) {
        Duration d = BASE_LOCK.multipliedBy(1L << Math.max(0, Math.min(level - 1, 10)));
        return d.compareTo(MAX_LOCK) > 0 ? MAX_LOCK : d;
    }
}
