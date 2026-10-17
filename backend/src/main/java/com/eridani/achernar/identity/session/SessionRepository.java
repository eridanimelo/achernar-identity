package com.eridani.achernar.identity.session;

import com.eridani.achernar.identity.shared.db.Db;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {

    /** Sessão com dados da aplicação, para listagem (RN-SES-02). */
    public record View(Session session, String clientId, String applicationName) {
    }

    private static final String COLUMNS = "s.id, s.account_id, s.application_id, s.amr, s.created_at, s.auth_time, "
            + "s.last_used_at, s.expires_at, s.last_reauth_at, s.revoked_at, s.revoke_reason, s.ip, s.user_agent";

    private static final RowMapper<Session> MAPPER = (rs, i) -> new Session(Db.uuid(rs, "id"),
            Db.uuid(rs, "account_id"), Db.uuid(rs, "application_id"), rs.getString("amr"),
            Db.instant(rs, "created_at"), Db.instant(rs, "auth_time"), Db.instant(rs, "last_used_at"),
            Db.instant(rs, "expires_at"), Db.instant(rs, "last_reauth_at"), Db.instant(rs, "revoked_at"),
            rs.getString("revoke_reason"), rs.getString("ip"), rs.getString("user_agent"));

    private final JdbcTemplate jdbc;

    public SessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Session s, List<String> audiences) {
        jdbc.update("INSERT INTO session (id, account_id, application_id, amr, created_at, auth_time, last_used_at, "
                        + "expires_at, last_reauth_at, ip, user_agent) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                s.id(), s.accountId(), s.applicationId(), s.amr(), Db.ts(s.createdAt()), Db.ts(s.authTime()),
                Db.ts(s.lastUsedAt()), Db.ts(s.expiresAt()), Db.ts(s.lastReauthAt()), s.ip(), s.userAgent());
        for (String a : audiences) {
            jdbc.update("INSERT INTO session_audience (session_id, audience) VALUES (?, ?)", s.id(), a);
        }
    }

    public Optional<Session> find(UUID id) {
        List<Session> rows = jdbc.query("SELECT " + COLUMNS + " FROM session s WHERE s.id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Session> findForUpdate(UUID id) {
        List<Session> rows = jdbc.query("SELECT " + COLUMNS + " FROM session s WHERE s.id = ? FOR UPDATE", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<String> audiences(UUID sessionId) {
        return jdbc.queryForList("SELECT audience FROM session_audience WHERE session_id = ? ORDER BY audience",
                String.class, sessionId);
    }

    public List<View> listActive(UUID accountId, Instant now) {
        return jdbc.query("SELECT " + COLUMNS + ", a.client_id AS app_client_id, a.name AS app_name FROM session s "
                        + "JOIN application a ON a.id = s.application_id WHERE s.account_id = ? AND s.revoked_at IS NULL "
                        + "AND s.expires_at > ? ORDER BY s.last_used_at DESC",
                (rs, i) -> new View(MAPPER.mapRow(rs, i), rs.getString("app_client_id"), rs.getString("app_name")),
                accountId, Db.ts(now));
    }

    public int countActive(UUID accountId, Instant now) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM session WHERE account_id = ? AND revoked_at IS NULL "
                + "AND expires_at > ?", Integer.class, accountId, Db.ts(now));
        return n == null ? 0 : n;
    }

    public Optional<UUID> oldestActive(UUID accountId, Instant now) {
        List<UUID> rows = jdbc.query("SELECT id FROM session WHERE account_id = ? AND revoked_at IS NULL "
                + "AND expires_at > ? ORDER BY last_used_at ASC LIMIT 1", (rs, i) -> Db.uuid(rs, "id"),
                accountId, Db.ts(now));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean revoke(UUID id, String reason, Instant now) {
        return jdbc.update("UPDATE session SET revoked_at = ?, revoke_reason = ? WHERE id = ? AND revoked_at IS NULL",
                Db.ts(now), reason, id) > 0;
    }

    /** Revoga todas as sessões ativas da conta (exceto {@code exceptId}); retorna os ids revogados. */
    public List<UUID> revokeAllForAccount(UUID accountId, String reason, Instant now, UUID exceptId) {
        if (exceptId == null) {
            return jdbc.query("UPDATE session SET revoked_at = ?, revoke_reason = ? WHERE account_id = ? "
                    + "AND revoked_at IS NULL RETURNING id", (rs, i) -> Db.uuid(rs, "id"), Db.ts(now), reason, accountId);
        }
        return jdbc.query("UPDATE session SET revoked_at = ?, revoke_reason = ? WHERE account_id = ? "
                + "AND revoked_at IS NULL AND id <> ? RETURNING id", (rs, i) -> Db.uuid(rs, "id"),
                Db.ts(now), reason, accountId, exceptId);
    }

    public int revokeAllForApplication(UUID applicationId, String reason, Instant now) {
        return jdbc.update("UPDATE session SET revoked_at = ?, revoke_reason = ? WHERE application_id = ? "
                + "AND revoked_at IS NULL", Db.ts(now), reason, applicationId);
    }

    /** Revoga todas as sessões ativas da instalação (chave comprometida — UC-072). */
    public int revokeAll(String reason, Instant now) {
        return jdbc.update("UPDATE session SET revoked_at = ?, revoke_reason = ? WHERE revoked_at IS NULL",
                Db.ts(now), reason);
    }

    public void touch(UUID id, Instant now) {
        jdbc.update("UPDATE session SET last_used_at = ? WHERE id = ?", Db.ts(now), id);
    }

    public void markReauth(UUID id, Instant now) {
        jdbc.update("UPDATE session SET last_reauth_at = ? WHERE id = ?", Db.ts(now), id);
    }

    public int purge(Instant cutoff, int batch) {
        return jdbc.update("DELETE FROM session WHERE id IN (SELECT id FROM session "
                + "WHERE COALESCE(revoked_at, expires_at) < ? LIMIT ?)", Db.ts(cutoff), batch);
    }
}
