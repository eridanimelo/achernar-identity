package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.db.PageResult;
import com.eridani.achernar.identity.shared.db.SortSpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {

    /** Linha de listagem (sem filhos). */
    public record Summary(UUID id, String clientId, String name, ClientType type, RegistryStatus status,
                          boolean allowRegistration, boolean identityApiAccess, Instant createdAt) {
    }

    public record SecretRow(UUID id, Instant createdAt, Instant lastUsedAt, Instant revokedAt) {
    }

    private static final String COLUMNS = "id, client_id, name, description, type, status, allow_registration, "
            + "identity_api_access, google_enabled, facebook_enabled, access_token_ttl_seconds, verify_email_url, "
            + "reset_password_url, confirm_email_change_url, is_system, created_at, updated_at, version";

    private static final Map<String, String> SORTS = Map.of(
            "name", "lower(name)", "client_id", "client_id", "created_at", "created_at", "status", "status");

    private final JdbcTemplate jdbc;

    public ApplicationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ leitura
    public Optional<Application> findByClientId(String clientId) {
        return load("SELECT " + COLUMNS + " FROM application WHERE client_id = ?", clientId);
    }

    public Optional<Application> findById(UUID id) {
        return load("SELECT " + COLUMNS + " FROM application WHERE id = ?", id);
    }

    private Optional<Application> load(String sql, Object arg) {
        List<Base> rows = jdbc.query(sql, (rs, i) -> map(rs), arg);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(withChildren(rows.get(0)));
    }

    private record Base(UUID id, String clientId, String name, String description, ClientType type,
                        RegistryStatus status, boolean allowRegistration, boolean identityApiAccess,
                        boolean googleEnabled, boolean facebookEnabled, Integer ttl, String verifyUrl,
                        String resetUrl, String changeUrl, boolean system, Instant createdAt, Instant updatedAt,
                        long version) {
    }

    private static Base map(ResultSet rs) throws SQLException {
        return new Base(Db.uuid(rs, "id"), rs.getString("client_id"), rs.getString("name"),
                rs.getString("description"), ClientType.valueOf(rs.getString("type")),
                RegistryStatus.valueOf(rs.getString("status")), rs.getBoolean("allow_registration"),
                rs.getBoolean("identity_api_access"), rs.getBoolean("google_enabled"),
                rs.getBoolean("facebook_enabled"), Db.intOrNull(rs, "access_token_ttl_seconds"),
                rs.getString("verify_email_url"), rs.getString("reset_password_url"),
                rs.getString("confirm_email_change_url"), rs.getBoolean("is_system"),
                Db.instant(rs, "created_at"), Db.instant(rs, "updated_at"), rs.getLong("version"));
    }

    private Application withChildren(Base b) {
        List<String> origins = jdbc.queryForList(
                "SELECT origin FROM application_origin WHERE application_id = ? ORDER BY origin", String.class, b.id());
        List<String> redirects = jdbc.queryForList(
                "SELECT uri FROM application_redirect_uri WHERE application_id = ? ORDER BY uri", String.class, b.id());
        List<AudienceGrant> audiences = jdbc.query(
                "SELECT aa.resource_api_id, r.audience, aa.is_default, r.status FROM application_audience aa "
                        + "JOIN resource_api r ON r.id = aa.resource_api_id WHERE aa.application_id = ? ORDER BY r.audience",
                (rs, i) -> new AudienceGrant(Db.uuid(rs, "resource_api_id"), rs.getString("audience"),
                        rs.getBoolean("is_default"), "ACTIVE".equals(rs.getString("status"))), b.id());
        return new Application(b.id(), b.clientId(), b.name(), b.description(), b.type(), b.status(),
                b.allowRegistration(), b.identityApiAccess(), b.googleEnabled(), b.facebookEnabled(), b.ttl(),
                b.verifyUrl(), b.resetUrl(), b.changeUrl(), b.system(), b.createdAt(), b.updatedAt(), b.version(),
                origins, redirects, audiences);
    }

    public PageResult<Summary> list(String q, String status, int page, int size, String sort) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status);
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + Db.escapeLike(q.strip().toLowerCase(java.util.Locale.ROOT)) + "%";
            where.append(" AND (lower(name) LIKE ? ESCAPE '\\' OR client_id LIKE ? ESCAPE '\\')");
            args.add(like);
            args.add(like);
        }
        String order = SortSpec.orderBy(SORTS, sort, "created_at DESC");
        Long total = jdbc.queryForObject("SELECT count(*) FROM application" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<Summary> items = jdbc.query("SELECT id, client_id, name, type, status, allow_registration, "
                        + "identity_api_access, created_at FROM application" + where + " ORDER BY " + order
                        + ", id LIMIT ? OFFSET ?",
                (rs, i) -> new Summary(Db.uuid(rs, "id"), rs.getString("client_id"), rs.getString("name"),
                        ClientType.valueOf(rs.getString("type")), RegistryStatus.valueOf(rs.getString("status")),
                        rs.getBoolean("allow_registration"), rs.getBoolean("identity_api_access"),
                        Db.instant(rs, "created_at")), pageArgs.toArray());
        return new PageResult<>(items, page, size, total == null ? 0 : total);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT count(*) FROM application", Long.class);
        return n == null ? 0 : n;
    }

    /** Origem pertence a alguma aplicação ativa? (CORS dinâmico, D-41). */
    public boolean originRegistered(String origin, boolean identityApiOnly) {
        String sql = "SELECT EXISTS (SELECT 1 FROM application_origin o JOIN application a ON a.id = o.application_id "
                + "WHERE a.status = 'ACTIVE' AND o.origin = ?" + (identityApiOnly ? " AND a.identity_api_access" : "") + ")";
        Boolean b = jdbc.queryForObject(sql, Boolean.class, origin);
        return Boolean.TRUE.equals(b);
    }

    // ------------------------------------------------------------------ escrita
    public void insert(Application a) {
        jdbc.update("INSERT INTO application (id, client_id, name, description, type, status, allow_registration, "
                        + "identity_api_access, google_enabled, facebook_enabled, access_token_ttl_seconds, "
                        + "verify_email_url, reset_password_url, confirm_email_change_url, is_system, created_at, "
                        + "updated_at, version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
                a.id(), a.clientId(), a.name(), a.description(), a.type().name(), a.status().name(),
                a.allowRegistration(), a.identityApiAccess(), a.googleEnabled(), a.facebookEnabled(),
                a.accessTokenTtlSeconds(), a.verifyEmailUrl(), a.resetPasswordUrl(), a.confirmEmailChangeUrl(),
                a.system(), Db.ts(a.createdAt()), Db.ts(a.updatedAt()));
        replaceChildren(a.id(), a.origins(), a.redirectUris(), a.audiences());
    }

    /** Atualiza com controle otimista; retorna false se a versão divergiu. */
    public boolean update(Application a, long expectedVersion) {
        int n = jdbc.update("UPDATE application SET name = ?, description = ?, status = ?, allow_registration = ?, "
                        + "identity_api_access = ?, google_enabled = ?, facebook_enabled = ?, "
                        + "access_token_ttl_seconds = ?, verify_email_url = ?, reset_password_url = ?, "
                        + "confirm_email_change_url = ?, updated_at = ?, version = version + 1 "
                        + "WHERE id = ? AND version = ?",
                a.name(), a.description(), a.status().name(), a.allowRegistration(), a.identityApiAccess(),
                a.googleEnabled(), a.facebookEnabled(), a.accessTokenTtlSeconds(), a.verifyEmailUrl(),
                a.resetPasswordUrl(), a.confirmEmailChangeUrl(), Db.ts(a.updatedAt()), a.id(), expectedVersion);
        if (n == 0) {
            return false;
        }
        replaceChildren(a.id(), a.origins(), a.redirectUris(), a.audiences());
        return true;
    }

    private void replaceChildren(UUID id, List<String> origins, List<String> redirects, List<AudienceGrant> auds) {
        jdbc.update("DELETE FROM application_origin WHERE application_id = ?", id);
        jdbc.update("DELETE FROM application_redirect_uri WHERE application_id = ?", id);
        jdbc.update("DELETE FROM application_audience WHERE application_id = ?", id);
        for (String o : origins) {
            jdbc.update("INSERT INTO application_origin (application_id, origin) VALUES (?, ?)", id, o);
        }
        for (String r : redirects) {
            jdbc.update("INSERT INTO application_redirect_uri (application_id, uri) VALUES (?, ?)", id, r);
        }
        for (AudienceGrant g : auds) {
            jdbc.update("INSERT INTO application_audience (application_id, resource_api_id, is_default) VALUES (?, ?, ?)",
                    id, g.apiId(), g.isDefault());
        }
    }

    // ------------------------------------------------------------------ segredos
    public void insertSecret(UUID id, UUID applicationId, String hash, Instant now) {
        jdbc.update("INSERT INTO application_secret (id, application_id, secret_hash, created_at) VALUES (?, ?, ?, ?)",
                id, applicationId, hash, Db.ts(now));
    }

    public List<SecretRow> listSecrets(UUID applicationId) {
        return jdbc.query("SELECT id, created_at, last_used_at, revoked_at FROM application_secret "
                        + "WHERE application_id = ? ORDER BY created_at",
                (rs, i) -> new SecretRow(Db.uuid(rs, "id"), Db.instant(rs, "created_at"),
                        Db.instant(rs, "last_used_at"), Db.instant(rs, "revoked_at")), applicationId);
    }

    public int countActiveSecrets(UUID applicationId) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM application_secret WHERE application_id = ? AND revoked_at IS NULL",
                Integer.class, applicationId);
        return n == null ? 0 : n;
    }

    public List<String> activeSecretHashes(UUID applicationId) {
        return jdbc.queryForList(
                "SELECT secret_hash FROM application_secret WHERE application_id = ? AND revoked_at IS NULL",
                String.class, applicationId);
    }

    public boolean revokeSecret(UUID applicationId, UUID secretId, Instant now) {
        return jdbc.update("UPDATE application_secret SET revoked_at = ? WHERE id = ? AND application_id = ? "
                + "AND revoked_at IS NULL", Db.ts(now), secretId, applicationId) > 0;
    }

    public void touchSecret(UUID applicationId, String hash, Instant now) {
        jdbc.update("UPDATE application_secret SET last_used_at = ? WHERE application_id = ? AND secret_hash = ? "
                + "AND (last_used_at IS NULL OR last_used_at < ?)", Db.ts(now), applicationId, hash,
                Db.ts(now.minusSeconds(3600)));
    }

    public boolean secretExists(UUID applicationId, UUID secretId) {
        Boolean b = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM application_secret WHERE id = ? AND application_id = ?)",
                Boolean.class, secretId, applicationId);
        return Boolean.TRUE.equals(b);
    }

    public boolean secretIsActive(UUID applicationId, UUID secretId) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM application_secret WHERE id = ? "
                + "AND application_id = ? AND revoked_at IS NULL)", Boolean.class, secretId, applicationId);
        return Boolean.TRUE.equals(b);
    }
}
