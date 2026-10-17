package com.eridani.achernar.identity.admin;

import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.db.PageResult;
import com.eridani.achernar.identity.shared.db.SortSpec;
import com.eridani.achernar.identity.shared.error.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas administrativas de contas (UC-080); apenas leitura, sem hashes ou segredos. */
@Repository
public class AdminAccountRepository {

    public record Row(UUID id, String email, String status, String role, boolean emailVerified, boolean hasPassword,
                      List<String> providers, Instant createdAt, String locale, Instant disabledAt,
                      String disabledReason, Instant anonymizedAt, long version) {
    }

    public record Filter(String q, String status, String role, String method, Instant createdFrom,
                         Instant createdTo) {
    }

    public record EventRow(UUID id, Instant occurredAt, String eventType, String severity, String outcome) {
    }

    private static final Map<String, String> SORTS = Map.of("created_at", "a.created_at", "email",
            "a.email_normalized", "status", "a.status");

    private static final String SELECT = "SELECT a.id, a.email_display, a.status, a.role, a.email_verified_at, "
            + "a.created_at, a.locale, a.disabled_at, a.disabled_reason, a.anonymized_at, a.version, "
            + "EXISTS (SELECT 1 FROM password_credential p WHERE p.account_id = a.id) AS has_password, "
            + "(SELECT string_agg(lower(l.provider), ',' ORDER BY l.provider) FROM social_link l "
            + "WHERE l.account_id = a.id) AS providers FROM account a";

    private final JdbcTemplate jdbc;

    public AdminAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Row map(java.sql.ResultSet rs) throws java.sql.SQLException {
        String prov = rs.getString("providers");
        return new Row(Db.uuid(rs, "id"), rs.getString("email_display"), rs.getString("status"), rs.getString("role"),
                rs.getTimestamp("email_verified_at") != null, rs.getBoolean("has_password"),
                prov == null || prov.isEmpty() ? List.of() : List.of(prov.split(",")),
                Db.instant(rs, "created_at"), rs.getString("locale"), Db.instant(rs, "disabled_at"),
                rs.getString("disabled_reason"), Db.instant(rs, "anonymized_at"), rs.getLong("version"));
    }

    public PageResult<Row> list(Filter f, int page, int size, String sort) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (f.q() != null) {
            where.append(" AND a.email_normalized LIKE ? ESCAPE '\\'");
            args.add(Db.escapeLike(f.q().toLowerCase(java.util.Locale.ROOT)) + "%");
        }
        if (f.status() != null) {
            where.append(" AND a.status = ?");
            args.add(f.status());
        }
        if (f.role() != null) {
            where.append(" AND a.role = ?");
            args.add(f.role());
        }
        if (f.method() != null) {
            if (f.method().equals("password")) {
                where.append(" AND EXISTS (SELECT 1 FROM password_credential p WHERE p.account_id = a.id)");
            } else {
                where.append(" AND EXISTS (SELECT 1 FROM social_link l WHERE l.account_id = a.id AND l.provider = ?)");
                args.add(f.method().toUpperCase(java.util.Locale.ROOT));
            }
        }
        if (f.createdFrom() != null) {
            where.append(" AND a.created_at >= ?");
            args.add(Db.ts(f.createdFrom()));
        }
        if (f.createdTo() != null) {
            where.append(" AND a.created_at <= ?");
            args.add(Db.ts(f.createdTo()));
        }
        String order = SortSpec.orderBy(SORTS, sort, "a.created_at DESC");
        Long total = jdbc.queryForObject("SELECT count(*) FROM account a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<Row> rows = jdbc.query(SELECT + where + " ORDER BY " + order + ", a.id LIMIT ? OFFSET ?",
                (rs, i) -> map(rs), pageArgs.toArray());
        return new PageResult<>(rows, page, size, total == null ? 0 : total);
    }

    public Row find(UUID id) {
        List<Row> rows = jdbc.query(SELECT + " WHERE a.id = ?", (rs, i) -> map(rs), id);
        if (rows.isEmpty()) {
            throw new ApiException(com.eridani.achernar.identity.shared.error.ErrorCode.NOT_FOUND);
        }
        return rows.get(0);
    }

    public int activeSessions(UUID accountId, Instant now) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM session WHERE account_id = ? AND revoked_at IS NULL "
                + "AND expires_at > ?", Integer.class, accountId, Db.ts(now));
        return n == null ? 0 : n;
    }

    public List<EventRow> recentEvents(UUID accountId, int limit) {
        return jdbc.query("SELECT id, occurred_at, event_type, severity, outcome FROM security_event "
                        + "WHERE account_id = ? ORDER BY occurred_at DESC, id DESC LIMIT ?",
                (rs, i) -> new EventRow(Db.uuid(rs, "id"), Db.instant(rs, "occurred_at"), rs.getString("event_type"),
                        rs.getString("severity"), rs.getString("outcome")), accountId, limit);
    }
}
