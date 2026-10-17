package com.eridani.achernar.identity.ratelimit;

import com.eridani.achernar.identity.shared.crypto.Sha256;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloqueio de login por e-mail normalizado, exista a conta ou não (D-48). Sem e-mail em claro na tabela
 * ({@code key_hash = sha256(email_normalized)}).
 */
@Service
public class LoginLockService {

    public record Failure(boolean newlyLocked, Duration lockDuration) {
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public LoginLockService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Segundos restantes de bloqueio (0 se livre). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long remainingLockSeconds(String emailNormalized) {
        LoginLockPolicy.State s = load(Sha256.hex(emailNormalized), false);
        return LoginLockPolicy.remainingSeconds(s, clock.instant());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Failure registerFailure(String emailNormalized) {
        String key = Sha256.hex(emailNormalized);
        Instant now = clock.instant();
        jdbc.update("INSERT INTO login_lock (key_hash, failure_count, window_start, lock_level) VALUES (?, 0, ?, 0) "
                + "ON CONFLICT (key_hash) DO NOTHING", key, Timestamp.from(now));
        LoginLockPolicy.State current = load(key, true);
        LoginLockPolicy.Result r = LoginLockPolicy.onFailure(current, now);
        LoginLockPolicy.State n = r.state();
        jdbc.update("UPDATE login_lock SET failure_count = ?, window_start = ?, locked_until = ?, lock_level = ?, "
                        + "last_lock_at = ? WHERE key_hash = ?",
                n.failureCount(), Timestamp.from(n.windowStart()),
                n.lockedUntil() == null ? null : Timestamp.from(n.lockedUntil()), n.lockLevel(),
                n.lastLockAt() == null ? null : Timestamp.from(n.lastLockAt()), key);
        return new Failure(r.newlyLocked(), r.lockDuration());
    }

    /** Zera contador e bloqueio (redefinição de senha concluída — RN-AUT-03). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String emailNormalized) {
        jdbc.update("DELETE FROM login_lock WHERE key_hash = ?", Sha256.hex(emailNormalized));
    }

    @Transactional
    public int purgeIdle(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM login_lock WHERE key_hash IN (SELECT key_hash FROM login_lock "
                        + "WHERE COALESCE(locked_until, window_start) < ? LIMIT ?)", Timestamp.from(cutoff), batch);
    }

    private LoginLockPolicy.State load(String key, boolean forUpdate) {
        String sql = "SELECT failure_count, window_start, locked_until, lock_level, last_lock_at FROM login_lock "
                + "WHERE key_hash = ?" + (forUpdate ? " FOR UPDATE" : "");
        List<LoginLockPolicy.State> rows = jdbc.query(sql, (rs, i) -> new LoginLockPolicy.State(
                rs.getInt("failure_count"),
                rs.getTimestamp("window_start").toInstant(),
                rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant(),
                rs.getInt("lock_level"),
                rs.getTimestamp("last_lock_at") == null ? null : rs.getTimestamp("last_lock_at").toInstant()), key);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
