package com.achernar.identity.auth;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActionRepository {
    public record Action(UUID id, UUID userId) {
    }

    private final NamedParameterJdbcTemplate db;

    public ActionRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public void create(UUID userId, String hash, String kind, Instant expiry) {
        db.update(
                "INSERT INTO email_action(id,user_id,token_hash,kind,expires_at) VALUES(:id,:uid,:hash,:kind,:expires)",
                Map.of("id", UUID.randomUUID(), "uid", userId, "hash", hash, "kind", kind, "expires",
                        java.sql.Timestamp.from(expiry)));
    }

    public Optional<Action> find(String hash, String kind) {
        return db.query(
                "SELECT id,user_id FROM email_action WHERE token_hash=:hash AND kind=:kind AND consumed_at IS NULL AND expires_at>CURRENT_TIMESTAMP",
                Map.of("hash", hash, "kind", kind),
                (r, n) -> new Action(r.getObject("id", UUID.class), r.getObject("user_id", UUID.class))).stream()
                .findFirst();
    }

    public boolean consume(UUID id) {
        return db.update(
                "UPDATE email_action SET consumed_at=CURRENT_TIMESTAMP WHERE id=:id AND consumed_at IS NULL AND expires_at>CURRENT_TIMESTAMP",
                Map.of("id", id)) == 1;
    }

    public void invalidate(UUID userId, String kind) {
        db.update("DELETE FROM email_action WHERE user_id=:id AND kind=:kind", Map.of("id", userId, "kind", kind));
    }

    public void remove(UUID userId) {
        db.update("DELETE FROM email_action WHERE user_id=:id", Map.of("id", userId));
    }
}
