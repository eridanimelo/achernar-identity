package com.eridani.achernar.identity.audit;

import java.util.Set;

/**
 * Catálogo de eventos de segurança (event-catalog.md). Cada tipo declara a severidade, o resultado padrão e a
 * lista permitida de chaves de {@code details} (RN-AUD-04): chaves fora da lista são descartadas.
 */
public enum EventType {
    ACCOUNT_REGISTERED(Severity.INFO, Outcome.SUCCESS, "locale"),
    REGISTRATION_DUPLICATE_ATTEMPT(Severity.NOTICE, Outcome.SUCCESS),
    EMAIL_VERIFIED(Severity.INFO, Outcome.SUCCESS),
    EMAIL_VERIFICATION_RESENT(Severity.INFO, Outcome.SUCCESS),
    LOGIN_SUCCEEDED(Severity.INFO, Outcome.SUCCESS, "amr"),
    LOGIN_FAILED(Severity.NOTICE, Outcome.FAILURE, "reason"),
    LOGIN_BLOCKED_UNVERIFIED(Severity.NOTICE, Outcome.FAILURE),
    LOGIN_BLOCKED_DISABLED(Severity.NOTICE, Outcome.FAILURE),
    ACCOUNT_LOCKED_THROTTLE(Severity.ALERT, Outcome.FAILURE, "lock_seconds"),
    SOCIAL_LOGIN_SUCCEEDED(Severity.INFO, Outcome.SUCCESS, "provider"),
    SOCIAL_LOGIN_FAILED(Severity.NOTICE, Outcome.FAILURE, "provider", "reason"),
    SOCIAL_ACCOUNT_CREATED(Severity.INFO, Outcome.SUCCESS, "provider", "email_verified"),
    SOCIAL_LINK_CONFLICT(Severity.NOTICE, Outcome.FAILURE, "provider"),
    PENDING_ACCOUNT_SUPERSEDED(Severity.NOTICE, Outcome.SUCCESS, "provider"),
    SOCIAL_LINKED(Severity.NOTICE, Outcome.SUCCESS, "provider"),
    SOCIAL_UNLINKED(Severity.NOTICE, Outcome.SUCCESS, "provider"),
    TOKEN_ISSUED(Severity.INFO, Outcome.SUCCESS, "audiences"),
    REFRESH_REUSE_TOLERATED(Severity.NOTICE, Outcome.SUCCESS),
    REFRESH_REUSE_DETECTED(Severity.ALERT, Outcome.FAILURE),
    LOGOUT(Severity.INFO, Outcome.SUCCESS),
    SESSION_REVOKED(Severity.INFO, Outcome.SUCCESS, "reason"),
    ALL_SESSIONS_REVOKED(Severity.NOTICE, Outcome.SUCCESS, "count", "include_current"),
    PASSWORD_RESET_REQUESTED(Severity.INFO, Outcome.SUCCESS),
    PASSWORD_RESET_COMPLETED(Severity.NOTICE, Outcome.SUCCESS),
    PASSWORD_CHANGED(Severity.NOTICE, Outcome.SUCCESS),
    PASSWORD_SET(Severity.NOTICE, Outcome.SUCCESS),
    REAUTH_SUCCEEDED(Severity.INFO, Outcome.SUCCESS, "method"),
    REAUTH_FAILED(Severity.NOTICE, Outcome.FAILURE, "method"),
    EMAIL_CHANGE_REQUESTED(Severity.NOTICE, Outcome.SUCCESS),
    EMAIL_CHANGED(Severity.ALERT, Outcome.SUCCESS),
    ACCOUNT_DISABLED(Severity.ALERT, Outcome.SUCCESS, "reason"),
    ACCOUNT_ENABLED(Severity.NOTICE, Outcome.SUCCESS),
    ADMIN_GRANTED(Severity.ALERT, Outcome.SUCCESS),
    ADMIN_REVOKED(Severity.ALERT, Outcome.SUCCESS),
    ACCOUNT_ANONYMIZED(Severity.ALERT, Outcome.SUCCESS),
    BOOTSTRAP_ADMIN_CREATED(Severity.ALERT, Outcome.SUCCESS),
    SETUP_ATTEMPT_FAILED(Severity.ALERT, Outcome.FAILURE),
    APPLICATION_CREATED(Severity.NOTICE, Outcome.SUCCESS, "client_id", "changed"),
    APPLICATION_UPDATED(Severity.NOTICE, Outcome.SUCCESS, "client_id", "changed"),
    APPLICATION_DISABLED(Severity.NOTICE, Outcome.SUCCESS, "client_id", "changed"),
    APPLICATION_ENABLED(Severity.NOTICE, Outcome.SUCCESS, "client_id", "changed"),
    APPLICATION_SECRET_ROTATED(Severity.ALERT, Outcome.SUCCESS, "client_id"),
    API_CREATED(Severity.NOTICE, Outcome.SUCCESS, "audience", "changed"),
    API_UPDATED(Severity.NOTICE, Outcome.SUCCESS, "audience", "changed"),
    API_DISABLED(Severity.NOTICE, Outcome.SUCCESS, "audience", "changed"),
    API_ENABLED(Severity.NOTICE, Outcome.SUCCESS, "audience", "changed"),
    API_CREDENTIAL_ROTATED(Severity.ALERT, Outcome.SUCCESS, "audience"),
    INTROSPECTION_AUTH_FAILED(Severity.NOTICE, Outcome.FAILURE, "audience"),
    SIGNING_KEY_ROTATED(Severity.ALERT, Outcome.SUCCESS, "kid", "mode"),
    SIGNING_KEY_REMOVED(Severity.ALERT, Outcome.SUCCESS, "kid", "emergency"),
    RATE_LIMIT_EXCEEDED(Severity.NOTICE, Outcome.FAILURE, "rule"),
    EMAIL_DELIVERY_FAILED(Severity.ALERT, Outcome.FAILURE, "template"),
    RETENTION_PURGE_RUN(Severity.INFO, Outcome.SUCCESS, "counts");

    private final Severity severity;
    private final Outcome defaultOutcome;
    private final Set<String> allowedDetails;

    EventType(Severity severity, Outcome defaultOutcome, String... allowedDetails) {
        this.severity = severity;
        this.defaultOutcome = defaultOutcome;
        this.allowedDetails = Set.of(allowedDetails);
    }

    public Severity severity() {
        return severity;
    }

    public Outcome defaultOutcome() {
        return defaultOutcome;
    }

    public boolean allows(String detailKey) {
        return allowedDetails.contains(detailKey);
    }
}
