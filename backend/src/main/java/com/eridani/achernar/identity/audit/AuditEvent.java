package com.eridani.achernar.identity.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Descrição de um evento a registrar; use {@link SecurityEventRecorder}. */
public final class AuditEvent {

    private final EventType type;
    private Outcome outcome;
    private UUID accountId;
    private UUID actorAccountId;
    private String clientId;
    private UUID sessionId;
    private final Map<String, Object> details = new LinkedHashMap<>();

    private AuditEvent(EventType type) {
        this.type = type;
        this.outcome = type.defaultOutcome();
    }

    public static AuditEvent of(EventType type) {
        return new AuditEvent(type);
    }

    public AuditEvent account(UUID id) {
        this.accountId = id;
        return this;
    }

    public AuditEvent actor(UUID id) {
        this.actorAccountId = id;
        return this;
    }

    public AuditEvent client(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AuditEvent session(UUID id) {
        this.sessionId = id;
        return this;
    }

    public AuditEvent outcome(Outcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public AuditEvent detail(String key, Object value) {
        if (value != null) {
            this.details.put(key, value);
        }
        return this;
    }

    public AuditEvent details(Map<String, ?> values) {
        if (values != null) {
            values.forEach(this::detail);
        }
        return this;
    }

    EventType type() {
        return type;
    }

    Outcome outcome() {
        return outcome;
    }

    UUID accountId() {
        return accountId;
    }

    UUID actorAccountId() {
        return actorAccountId;
    }

    String clientId() {
        return clientId;
    }

    UUID sessionId() {
        return sessionId;
    }

    Map<String, Object> details() {
        return details;
    }
}
