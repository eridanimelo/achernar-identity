package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SocialTransactionRepository {

    /** Resultado do consumo do {@code state}. */
    public enum ConsumeStatus { CONSUMED, UNKNOWN, REPLAY }

    public record Consumed(ConsumeStatus status, SocialTransaction transaction) {
    }

    private static final RowMapper<SocialTransaction> MAPPER = (rs, i) -> new SocialTransaction(
            Db.uuid(rs, "id"), Provider.valueOf(rs.getString("provider")),
            SocialTransaction.Purpose.valueOf(rs.getString("purpose")), Db.uuid(rs, "application_id"),
            rs.getString("redirect_uri"), rs.getString("client_state"), rs.getString("client_code_challenge"),
            rs.getString("audiences"), rs.getString("nonce"), rs.getBytes("provider_code_verifier_enc"),
            rs.getString("browser_binding_hash"), Db.uuid(rs, "account_id"), Db.uuid(rs, "session_id"),
            Db.instant(rs, "created_at"), Db.instant(rs, "expires_at"));

    private static final String COLUMNS = "id, provider, purpose, application_id, redirect_uri, client_state, "
            + "client_code_challenge, audiences, nonce, provider_code_verifier_enc, browser_binding_hash, "
            + "account_id, session_id, created_at, expires_at";

    private final JdbcTemplate jdbc;

    public SocialTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(SocialTransaction t, String stateHash) {
        jdbc.update("INSERT INTO social_transaction (id, state_hash, provider, purpose, application_id, redirect_uri, "
                        + "client_state, client_code_challenge, audiences, nonce, provider_code_verifier_enc, "
                        + "browser_binding_hash, account_id, session_id, created_at, expires_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                t.id(), stateHash, t.provider().name(), t.purpose().name(), t.applicationId(), t.redirectUri(),
                t.clientState(), t.clientCodeChallenge(), t.audiences(), t.nonce(), t.providerCodeVerifierEnc(),
                t.browserBindingHash(), t.accountId(), t.sessionId(), Db.ts(t.createdAt()), Db.ts(t.expiresAt()));
    }

    /** Consome atomicamente (uso único): diferencia {@code state} desconhecido de reutilizado. */
    public Consumed consume(String stateHash, Instant now) {
        List<SocialTransaction> rows = jdbc.query("UPDATE social_transaction SET consumed_at = ? "
                + "WHERE state_hash = ? AND consumed_at IS NULL RETURNING " + COLUMNS, MAPPER, Db.ts(now), stateHash);
        if (!rows.isEmpty()) {
            return new Consumed(ConsumeStatus.CONSUMED, rows.get(0));
        }
        Boolean exists = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM social_transaction WHERE state_hash = ?)",
                Boolean.class, stateHash);
        return new Consumed(Boolean.TRUE.equals(exists) ? ConsumeStatus.REPLAY : ConsumeStatus.UNKNOWN, null);
    }

    public int purgeOlderThan(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM social_transaction WHERE id IN (SELECT id FROM social_transaction "
                + "WHERE expires_at < ? LIMIT ?)", Db.ts(cutoff), batch);
    }

    public Optional<SocialTransaction> findById(UUID id) {
        List<SocialTransaction> rows = jdbc.query("SELECT " + COLUMNS + " FROM social_transaction WHERE id = ?",
                MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
