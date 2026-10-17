package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.crypto.TokenGenerator;
import com.eridani.achernar.identity.shared.db.Db;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tokens de uso único (D-17): 256 bits, hash SHA-256, um ativo por (conta, finalidade), consumo atômico.
 */
@Service
public class OneTimeTokenService {

    public record Issued(String token, Instant expiresAt) {
    }

    public record Consumed(UUID id, UUID accountId, TokenPurpose purpose, UUID applicationId,
                           String newEmailDisplay, String newEmailNormalized) {
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public OneTimeTokenService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Emite novo token invalidando o ativo anterior da mesma finalidade. Exige transação ativa. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Issued issue(UUID accountId, TokenPurpose purpose, UUID applicationId, String newEmailDisplay,
                        String newEmailNormalized, Duration ttl) {
        Instant now = clock.instant();
        Db.advisoryXactLock(jdbc, "ott:" + accountId + ":" + purpose);
        jdbc.update("UPDATE one_time_token SET invalidated_at = ? WHERE account_id = ? AND purpose = ? "
                + "AND consumed_at IS NULL AND invalidated_at IS NULL", Db.ts(now), accountId, purpose.name());
        String token = TokenGenerator.newToken();
        Instant expires = now.plus(ttl);
        jdbc.update("INSERT INTO one_time_token (id, account_id, purpose, token_hash, application_id, "
                        + "new_email_display, new_email_normalized, created_at, expires_at) VALUES (?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), accountId, purpose.name(), Sha256.hex(token), applicationId, newEmailDisplay,
                newEmailNormalized, Db.ts(now), Db.ts(expires));
        return new Issued(token, expires);
    }

    /** Consome atomicamente; vazio para inexistente, expirado, consumido, invalidado ou de outra finalidade. */
    @Transactional
    public Optional<Consumed> consume(String token, TokenPurpose purpose) {
        if (token == null || token.length() < 43 || token.length() > 128) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        List<Consumed> rows = jdbc.query("UPDATE one_time_token SET consumed_at = ? WHERE token_hash = ? "
                        + "AND purpose = ? AND consumed_at IS NULL AND invalidated_at IS NULL AND expires_at > ? "
                        + "RETURNING id, account_id, purpose, application_id, new_email_display, new_email_normalized",
                (rs, i) -> new Consumed(Db.uuid(rs, "id"), Db.uuid(rs, "account_id"),
                        TokenPurpose.valueOf(rs.getString("purpose")), Db.uuid(rs, "application_id"),
                        rs.getString("new_email_display"), rs.getString("new_email_normalized")),
                Db.ts(now), Sha256.hex(token), purpose.name(), Db.ts(now));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Invalida tokens ativos da conta nas finalidades dadas (RN-PWD-05, RN-ACC-12). */
    @Transactional
    public int invalidate(UUID accountId, TokenPurpose... purposes) {
        Instant now = clock.instant();
        int total = 0;
        for (TokenPurpose p : purposes) {
            total += jdbc.update("UPDATE one_time_token SET invalidated_at = ? WHERE account_id = ? AND purpose = ? "
                    + "AND consumed_at IS NULL AND invalidated_at IS NULL", Db.ts(now), accountId, p.name());
        }
        return total;
    }

    @Transactional
    public int invalidateAll(UUID accountId) {
        return invalidate(accountId, TokenPurpose.values());
    }

    @Transactional
    public int purge(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM one_time_token WHERE id IN (SELECT id FROM one_time_token "
                + "WHERE COALESCE(consumed_at, invalidated_at, expires_at) < ? LIMIT ?)", Db.ts(cutoff), batch);
    }
}
