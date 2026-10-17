package com.eridani.achernar.identity.session;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    public record Row(UUID id, UUID sessionId, String tokenHash, Instant createdAt, Instant expiresAt,
                      Instant rotatedAt, UUID replacedById) {
        public boolean current() {
            return rotatedAt == null;
        }
    }

    private final JdbcTemplate jdbc;

    public RefreshTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID sessionId, String hash, Instant createdAt, Instant expiresAt) {
        jdbc.update("INSERT INTO refresh_token (id, session_id, token_hash, created_at, expires_at) VALUES (?,?,?,?,?)",
                id, sessionId, hash, Db.ts(createdAt), Db.ts(expiresAt));
    }

    /** Localiza por hash travando a linha ({@code FOR UPDATE}) — serializa abas/requisições concorrentes (D-43). */
    public Optional<Row> findByHashForUpdate(String hash) {
        List<Row> rows = jdbc.query("SELECT id, session_id, token_hash, created_at, expires_at, rotated_at, "
                        + "replaced_by_id FROM refresh_token WHERE token_hash = ? FOR UPDATE",
                (rs, i) -> new Row(Db.uuid(rs, "id"), Db.uuid(rs, "session_id"), rs.getString("token_hash"),
                        Db.instant(rs, "created_at"), Db.instant(rs, "expires_at"), Db.instant(rs, "rotated_at"),
                        Db.uuid(rs, "replaced_by_id")), hash);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<UUID> currentIdOf(UUID sessionId) {
        List<UUID> rows = jdbc.query("SELECT id FROM refresh_token WHERE session_id = ? AND rotated_at IS NULL",
                (rs, i) -> Db.uuid(rs, "id"), sessionId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void markRotated(UUID id, Instant now) {
        jdbc.update("UPDATE refresh_token SET rotated_at = ? WHERE id = ? AND rotated_at IS NULL", Db.ts(now), id);
    }

    public void setReplacedBy(UUID id, UUID replacedBy) {
        jdbc.update("UPDATE refresh_token SET replaced_by_id = ? WHERE id = ?", replacedBy, id);
    }
}
