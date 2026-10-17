package com.eridani.achernar.identity.credential;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordCredentialRepository {

    public record Credential(UUID accountId, String hash, Instant changedAt) {
    }

    private final JdbcTemplate jdbc;

    public PasswordCredentialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Credential> find(UUID accountId) {
        List<Credential> rows = jdbc.query("SELECT account_id, password_hash, changed_at FROM password_credential "
                        + "WHERE account_id = ?",
                (rs, i) -> new Credential(Db.uuid(rs, "account_id"), rs.getString("password_hash"),
                        Db.instant(rs, "changed_at")), accountId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean exists(UUID accountId) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM password_credential WHERE account_id = ?)",
                Boolean.class, accountId);
        return Boolean.TRUE.equals(b);
    }

    public void upsert(UUID accountId, String hash, Instant now) {
        jdbc.update("INSERT INTO password_credential (account_id, password_hash, changed_at, created_at) "
                + "VALUES (?, ?, ?, ?) ON CONFLICT (account_id) DO UPDATE SET password_hash = EXCLUDED.password_hash, "
                + "changed_at = EXCLUDED.changed_at", accountId, hash, Db.ts(now), Db.ts(now));
    }

    /** Regrava o hash com parâmetros novos sem alterar {@code changed_at} (RN-AUT-07). */
    public void rehash(UUID accountId, String hash) {
        jdbc.update("UPDATE password_credential SET password_hash = ? WHERE account_id = ?", hash, accountId);
    }

    public void delete(UUID accountId) {
        jdbc.update("DELETE FROM password_credential WHERE account_id = ?", accountId);
    }
}
