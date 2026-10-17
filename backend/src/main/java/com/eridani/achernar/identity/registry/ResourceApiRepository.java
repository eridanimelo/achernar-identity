package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.db.PageResult;
import com.eridani.achernar.identity.shared.db.SortSpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceApiRepository {

    private static final String COLUMNS = "id, audience, name, description, status, created_at, updated_at, version";
    private static final Map<String, String> SORTS = Map.of(
            "name", "lower(name)", "audience", "audience", "created_at", "created_at", "status", "status");

    private static final RowMapper<ResourceApi> MAPPER = (rs, i) -> new ResourceApi(Db.uuid(rs, "id"),
            rs.getString("audience"), rs.getString("name"), rs.getString("description"),
            RegistryStatus.valueOf(rs.getString("status")), Db.instant(rs, "created_at"),
            Db.instant(rs, "updated_at"), rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public ResourceApiRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ResourceApi> findById(UUID id) {
        return one("SELECT " + COLUMNS + " FROM resource_api WHERE id = ?", id);
    }

    public Optional<ResourceApi> findByAudience(String audience) {
        return one("SELECT " + COLUMNS + " FROM resource_api WHERE audience = ?", audience);
    }

    private Optional<ResourceApi> one(String sql, Object arg) {
        List<ResourceApi> rows = jdbc.query(sql, MAPPER, arg);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public PageResult<ResourceApi> list(String q, String status, int page, int size, String sort) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status);
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + Db.escapeLike(q.strip().toLowerCase(java.util.Locale.ROOT)) + "%";
            where.append(" AND (lower(name) LIKE ? ESCAPE '\\' OR lower(audience) LIKE ? ESCAPE '\\')");
            args.add(like);
            args.add(like);
        }
        String order = SortSpec.orderBy(SORTS, sort, "created_at DESC");
        Long total = jdbc.queryForObject("SELECT count(*) FROM resource_api" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<ResourceApi> items = jdbc.query("SELECT " + COLUMNS + " FROM resource_api" + where + " ORDER BY "
                + order + ", id LIMIT ? OFFSET ?", MAPPER, pageArgs.toArray());
        return new PageResult<>(items, page, size, total == null ? 0 : total);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT count(*) FROM resource_api", Long.class);
        return n == null ? 0 : n;
    }

    public void insert(ResourceApi a) {
        jdbc.update("INSERT INTO resource_api (id, audience, name, description, status, created_at, updated_at, "
                        + "version) VALUES (?,?,?,?,?,?,?,0)",
                a.id(), a.audience(), a.name(), a.description(), a.status().name(), Db.ts(a.createdAt()),
                Db.ts(a.updatedAt()));
    }

    public boolean update(ResourceApi a, long expectedVersion) {
        return jdbc.update("UPDATE resource_api SET name = ?, description = ?, status = ?, updated_at = ?, "
                        + "version = version + 1 WHERE id = ? AND version = ?",
                a.name(), a.description(), a.status().name(), Db.ts(a.updatedAt()), a.id(), expectedVersion) > 0;
    }

    /** Resolve audiências (texto) → APIs existentes, na ordem informada. */
    public List<ResourceApi> findByAudiences(List<String> audiences) {
        List<ResourceApi> out = new ArrayList<>();
        for (String a : audiences) {
            findByAudience(a).ifPresent(out::add);
        }
        return out;
    }

    // ------------------------------------------------------------------ credenciais
    public record CredentialRow(UUID id, Instant createdAt, Instant lastUsedAt, Instant revokedAt) {
    }

    public void insertCredential(UUID id, UUID apiId, String hash, Instant now) {
        jdbc.update("INSERT INTO resource_api_secret (id, resource_api_id, secret_hash, created_at) VALUES (?,?,?,?)",
                id, apiId, hash, Db.ts(now));
    }

    public List<CredentialRow> listCredentials(UUID apiId) {
        return jdbc.query("SELECT id, created_at, last_used_at, revoked_at FROM resource_api_secret "
                        + "WHERE resource_api_id = ? ORDER BY created_at",
                (rs, i) -> new CredentialRow(Db.uuid(rs, "id"), Db.instant(rs, "created_at"),
                        Db.instant(rs, "last_used_at"), Db.instant(rs, "revoked_at")), apiId);
    }

    public int countActiveCredentials(UUID apiId) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM resource_api_secret WHERE resource_api_id = ? AND revoked_at IS NULL",
                Integer.class, apiId);
        return n == null ? 0 : n;
    }

    public List<String> activeCredentialHashes(UUID apiId) {
        return jdbc.queryForList("SELECT secret_hash FROM resource_api_secret WHERE resource_api_id = ? "
                + "AND revoked_at IS NULL", String.class, apiId);
    }

    public boolean credentialIsActive(UUID apiId, UUID credentialId) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM resource_api_secret WHERE id = ? "
                + "AND resource_api_id = ? AND revoked_at IS NULL)", Boolean.class, credentialId, apiId);
        return Boolean.TRUE.equals(b);
    }

    public boolean credentialExists(UUID apiId, UUID credentialId) {
        Boolean b = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM resource_api_secret WHERE id = ? "
                + "AND resource_api_id = ?)", Boolean.class, credentialId, apiId);
        return Boolean.TRUE.equals(b);
    }

    public boolean revokeCredential(UUID apiId, UUID credentialId, Instant now) {
        return jdbc.update("UPDATE resource_api_secret SET revoked_at = ? WHERE id = ? AND resource_api_id = ? "
                + "AND revoked_at IS NULL", Db.ts(now), credentialId, apiId) > 0;
    }

    public void touchCredential(UUID apiId, String hash, Instant now) {
        jdbc.update("UPDATE resource_api_secret SET last_used_at = ? WHERE resource_api_id = ? AND secret_hash = ? "
                + "AND (last_used_at IS NULL OR last_used_at < ?)", Db.ts(now), apiId, hash,
                Db.ts(now.minusSeconds(3600)));
    }
}
