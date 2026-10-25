package com.achernar.identity.shared.audit;

import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Audit {
    private final NamedParameterJdbcTemplate db;

    public Audit(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public void record(UUID actor, UUID target, String action) {
        var args = new HashMap<String, Object>();
        args.put("id", UUID.randomUUID());
        args.put("actor", actor);
        args.put("target", target);
        args.put("action", action);
        db.update("INSERT INTO security_audit_event(id,actor_id,target_id,action) VALUES(:id,:actor,:target,:action)",
                args);
    }
}
