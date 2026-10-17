package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Códigos de troca próprios do Identity (data-model §2.10). */
@Repository
public class SocialExchangeCodeRepository {

    public record Code(UUID id, UUID accountId, UUID applicationId, Provider provider, String audiences,
                       String redirectUri, String codeChallenge, Instant createdAt, Instant expiresAt,
                       Instant consumedAt, UUID sessionId) {
    }

    private static final RowMapper<Code> MAPPER = (rs, i) -> new Code(Db.uuid(rs, "id"), Db.uuid(rs, "account_id"),
            Db.uuid(rs, "application_id"), Provider.valueOf(rs.getString("provider")), rs.getString("audiences"),
            rs.getString("redirect_uri"), rs.getString("code_challenge"), Db.instant(rs, "created_at"),
            Db.instant(rs, "expires_at"), Db.instant(rs, "consumed_at"), Db.uuid(rs, "session_id"));

    private static final String COLUMNS = "id, account_id, application_id, provider, audiences, redirect_uri, "
            + "code_challenge, created_at, expires_at, consumed_at, session_id";

    private final JdbcTemplate jdbc;

    public SocialExchangeCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Code c, String codeHash) {
        jdbc.update("INSERT INTO social_exchange_code (id, code_hash, account_id, application_id, provider, "
                        + "audiences, redirect_uri, code_challenge, created_at, expires_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                c.id(), codeHash, c.accountId(), c.applicationId(), c.provider().name(), c.audiences(),
                c.redirectUri(), c.codeChallenge(), Db.ts(c.createdAt()), Db.ts(c.expiresAt()));
    }

    /** Trava a linha do código (serializa trocas simultâneas — CA-FT07-17). */
    public Optional<Code> findByHashForUpdate(String codeHash) {
        List<Code> rows = jdbc.query("SELECT " + COLUMNS + " FROM social_exchange_code WHERE code_hash = ? FOR UPDATE",
                MAPPER, codeHash);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void markConsumed(UUID id, UUID sessionId, Instant now) {
        jdbc.update("UPDATE social_exchange_code SET consumed_at = ?, session_id = ? WHERE id = ?", Db.ts(now),
                sessionId, id);
    }

    public int purgeOlderThan(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM social_exchange_code WHERE id IN (SELECT id FROM social_exchange_code "
                + "WHERE expires_at < ? LIMIT ?)", Db.ts(cutoff), batch);
    }
}
