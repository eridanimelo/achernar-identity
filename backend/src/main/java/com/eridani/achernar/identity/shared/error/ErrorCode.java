package com.eridani.achernar.identity.shared.error;

/** Catálogo de erros estável (error-catalog.md). {@code code()} é o contrato com os clientes. */
public enum ErrorCode {
    VALIDATION_ERROR(400),
    PASSWORD_POLICY_VIOLATION(400),
    MALFORMED_REQUEST(400),
    UNSUPPORTED_MEDIA_TYPE(415),
    PAYLOAD_TOO_LARGE(413),
    INVALID_CREDENTIALS(401),
    INVALID_CLIENT(401),
    ORIGIN_NOT_ALLOWED(403),
    REDIRECT_URI_NOT_ALLOWED(400),
    AUDIENCE_NOT_ALLOWED(400),
    REGISTRATION_DISABLED(403),
    PROVIDER_NOT_ENABLED(400),
    EMAIL_NOT_VERIFIED(403),
    ACCOUNT_DISABLED(403),
    INVALID_REFRESH_TOKEN(401),
    SESSION_REVOKED(401),
    UNAUTHENTICATED(401),
    TOKEN_INVALID_OR_EXPIRED(400),
    INVALID_GRANT(400),
    REAUTH_REQUIRED(403),
    REAUTH_FAILED(403),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    LAST_ADMIN_PROTECTED(409),
    LAST_ACCESS_METHOD(409),
    SOCIAL_ALREADY_LINKED(409),
    SOCIAL_EMAIL_UNAVAILABLE(400),
    ACCOUNT_LINK_REQUIRED(400),
    SETUP_TOKEN_INVALID(403),
    SETUP_NOT_AVAILABLE(404),
    RATE_LIMITED(429),
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }

    public String code() {
        return name();
    }

    /** Sufixo do {@code type}: {@code https://<issuer>/problems/<kebab>}. */
    public String typeSlug() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
