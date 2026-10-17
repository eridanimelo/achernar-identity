package com.eridani.achernar.identity.admin;

import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.json.MiniJson;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.util.Time;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-090 — consulta de eventos de segurança com paginação por cursor (keyset). */
@Service
public class SecurityEventQuery {

    static final Duration MAX_RANGE = Duration.ofDays(90);
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 100;

    public record Criteria(Instant from, Instant to, List<String> types, String account, String clientId,
                           String outcome, String severity, Integer limit, String cursor) {
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public SecurityEventQuery(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(Criteria c) {
        Instant now = clock.instant();
        Instant to = c.to() != null ? c.to() : now;
        Instant from = c.from() != null ? c.from() : to.minus(Duration.ofHours(24));
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw ApiException.validation("from", "INVALID_RANGE");
        }
        int limit = c.limit() == null ? DEFAULT_LIMIT : c.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw ApiException.validation("limit", "OUT_OF_RANGE");
        }
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE e.occurred_at >= ? AND e.occurred_at <= ?");
        args.add(Db.ts(from));
        args.add(Db.ts(to));
        if (c.types() != null && !c.types().isEmpty()) {
            where.append(" AND e.event_type IN (").append("?,".repeat(c.types().size()), 0,
                    c.types().size() * 2 - 1).append(')');
            for (String t : c.types()) {
                try {
                    EventType.valueOf(t);
                } catch (IllegalArgumentException ex) {
                    throw ApiException.validation("type", "UNKNOWN");
                }
                args.add(t);
            }
        }
        if (c.account() != null) {
            UUID accountId = resolveAccount(c.account());
            where.append(" AND e.account_id = ?");
            args.add(accountId);
        }
        if (c.clientId() != null) {
            where.append(" AND e.client_id = ?");
            args.add(c.clientId());
        }
        if (c.outcome() != null) {
            if (!List.of("SUCCESS", "FAILURE").contains(c.outcome())) {
                throw ApiException.validation("outcome", "INVALID");
            }
            where.append(" AND e.outcome = ?");
            args.add(c.outcome());
        }
        if (c.severity() != null) {
            if (!List.of("INFO", "NOTICE", "ALERT").contains(c.severity())) {
                throw ApiException.validation("severity", "INVALID");
            }
            where.append(" AND e.severity = ?");
            args.add(c.severity());
        }
        if (c.cursor() != null && !c.cursor().isBlank()) {
            EventCursor cur = EventCursor.decode(c.cursor());
            where.append(" AND (e.occurred_at, e.id) < (?, ?)");
            args.add(Db.ts(cur.at()));
            args.add(cur.id());
        }
        args.add(limit + 1);
        List<Row> rows = jdbc.query("SELECT e.id, e.occurred_at, e.event_type, e.severity, e.outcome, e.account_id, "
                + "e.actor_account_id, e.client_id, e.session_id, e.ip, e.user_agent, e.request_id, e.details, "
                + "a.email_display FROM security_event e LEFT JOIN account a ON a.id = e.account_id" + where
                + " ORDER BY e.occurred_at DESC, e.id DESC LIMIT ?", (rs, i) -> new Row(Db.uuid(rs, "id"),
                Db.instant(rs, "occurred_at"), rs.getString("event_type"), rs.getString("severity"),
                rs.getString("outcome"), Db.uuid(rs, "account_id"), rs.getString("email_display"),
                Db.uuid(rs, "actor_account_id"), rs.getString("client_id"), Db.uuid(rs, "session_id"),
                rs.getString("ip"), rs.getString("user_agent"), rs.getString("request_id"),
                rs.getString("details")), args.toArray());
        String next = null;
        if (rows.size() > limit) {
            Row last = rows.get(limit - 1);
            next = new EventCursor(last.occurredAt(), last.id()).encode();
            rows = rows.subList(0, limit);
        }
        Obj body = Obj.of().with("items", rows.stream().map(SecurityEventQuery::item).toList())
                .withNullable("next_cursor", next);
        return body;
    }

    private record Row(UUID id, Instant occurredAt, String type, String severity, String outcome, UUID accountId,
                       String email, UUID actorId, String clientId, UUID sessionId, String ip, String userAgent,
                       String requestId, String details) {
    }

    private static Obj item(Row r) {
        Obj account = r.accountId() == null ? null : Obj.of().with("id", r.accountId().toString())
                .withNullable("email", r.email());
        Object details;
        try {
            details = MiniJson.parseObject(r.details());
        } catch (RuntimeException e) {
            details = Map.of();
        }
        return Obj.of().with("id", r.id().toString()).with("occurred_at", Time.iso(r.occurredAt()))
                .with("event_type", r.type()).with("severity", r.severity()).with("outcome", r.outcome())
                .with("account", account).with("actor_account_id", r.actorId() == null ? null : r.actorId().toString())
                .with("client_id", r.clientId()).with("session_id", r.sessionId() == null ? null : r.sessionId().toString())
                .with("ip", r.ip()).with("user_agent", r.userAgent()).with("request_id", r.requestId())
                .with("details", details);
    }

    private UUID resolveAccount(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            // não é UUID: tenta e-mail exato
        }
        EmailAddress.Normalized email = EmailAddress.parse(raw)
                .orElseThrow(() -> ApiException.validation("account", "INVALID_FORMAT"));
        List<UUID> ids = jdbc.query("SELECT id FROM account WHERE email_normalized = ?",
                (rs, i) -> Db.uuid(rs, "id"), email.normalized());
        return ids.isEmpty() ? new UUID(0, 0) : ids.get(0); // nenhum resultado, sem revelar inexistência
    }

}
