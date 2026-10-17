package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SigningKeyRepository {

    private static final String COLUMNS = "id, kid, public_jwk, private_key_enc, status, created_at, publish_at, "
            + "activate_at, retire_at, remove_at, removal_reason";

    private static final RowMapper<SigningKey> MAPPER = (rs, i) -> new SigningKey(Db.uuid(rs, "id"),
            rs.getString("kid"), rs.getString("public_jwk"), rs.getBytes("private_key_enc"),
            KeyStatus.valueOf(rs.getString("status")), Db.instant(rs, "created_at"), Db.instant(rs, "publish_at"),
            Db.instant(rs, "activate_at"), Db.instant(rs, "retire_at"), Db.instant(rs, "remove_at"),
            rs.getString("removal_reason"));

    private final JdbcTemplate jdbc;

    public SigningKeyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SigningKey> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM signing_key ORDER BY created_at DESC", MAPPER);
    }

    /** Chaves no JWKS: {@code PUBLISHED}, {@code ACTIVE} e {@code RETIRED} (RN-TOK-13). */
    public List<SigningKey> findPublishable() {
        return jdbc.query("SELECT " + COLUMNS + " FROM signing_key WHERE status IN ('PUBLISHED','ACTIVE','RETIRED') "
                + "ORDER BY created_at DESC", MAPPER);
    }

    public Optional<SigningKey> findActive() {
        List<SigningKey> rows = jdbc.query("SELECT " + COLUMNS + " FROM signing_key WHERE status = 'ACTIVE'", MAPPER);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<SigningKey> findByKid(String kid) {
        List<SigningKey> rows = jdbc.query("SELECT " + COLUMNS + " FROM signing_key WHERE kid = ?", MAPPER, kid);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void insert(SigningKey k) {
        jdbc.update("INSERT INTO signing_key (id, kid, algorithm, public_jwk, private_key_enc, status, created_at, "
                        + "publish_at, activate_at, retire_at, remove_at, removal_reason) "
                        + "VALUES (?,?,'RS256',?,?,?,?,?,?,?,?,?)",
                k.id(), k.kid(), k.publicJwk(), k.privateKeyEnc(), k.status().name(), Db.ts(k.createdAt()),
                Db.ts(k.publishAt()), Db.ts(k.activateAt()), Db.ts(k.retireAt()), Db.ts(k.removeAt()),
                k.removalReason());
    }

    public void activate(String kid, Instant now) {
        jdbc.update("UPDATE signing_key SET status = 'ACTIVE', activate_at = ? WHERE kid = ?", Db.ts(now), kid);
    }

    public void retire(String kid, Instant now) {
        jdbc.update("UPDATE signing_key SET status = 'RETIRED', retire_at = ? WHERE kid = ? AND status = 'ACTIVE'",
                Db.ts(now), kid);
    }

    /** Marca como removida e apaga a chave privada. */
    public void remove(String kid, Instant now, String reason) {
        jdbc.update("UPDATE signing_key SET status = 'REMOVED', private_key_enc = NULL, remove_at = ?, "
                + "removal_reason = ? WHERE kid = ?", Db.ts(now), reason, kid);
    }

    public int purgeRemoved(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM signing_key WHERE id IN (SELECT id FROM signing_key "
                + "WHERE status = 'REMOVED' AND remove_at < ? LIMIT ?)", Db.ts(cutoff), batch);
    }
}
