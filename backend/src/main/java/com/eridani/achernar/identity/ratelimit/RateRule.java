package com.eridani.achernar.identity.ratelimit;

import java.time.Duration;

/** Regras de janela fixa (parameters.md §5, P-30…P-45). */
public enum RateRule {
    /** P-31: tentativas por IP em rotas de autenticação. */
    AUTH_IP_MINUTE("auth:ip:min", Duration.ofMinutes(1), 20),
    AUTH_IP_HOUR("auth:ip:hour", Duration.ofHours(1), 200),
    /** P-32 */
    REGISTER_IP("register:ip", Duration.ofHours(1), 10),
    REGISTER_EMAIL("register:email", Duration.ofHours(1), 3),
    /** Cadastro duplicado: um TPL-05 por hora por destinatário. */
    DUPLICATE_NOTICE_EMAIL("dupnotice:email", Duration.ofHours(1), 1),
    /** P-33 */
    FORGOT_EMAIL("forgot:email", Duration.ofHours(1), 3),
    FORGOT_IP("forgot:ip", Duration.ofHours(1), 10),
    /** P-34 */
    RESEND_EMAIL_MINUTE("resend:email:min", Duration.ofSeconds(60), 1),
    RESEND_EMAIL_HOUR("resend:email:hour", Duration.ofHours(1), 5),
    RESEND_IP("resend:ip", Duration.ofHours(1), 10),
    /** P-35 */
    EMAIL_CHANGE_ACCOUNT("emailchange:account", Duration.ofDays(1), 3),
    /** P-36 */
    REFRESH_SESSION("refresh:sid", Duration.ofMinutes(1), 30),
    REFRESH_IP("refresh:ip", Duration.ofMinutes(1), 120),
    /** P-37 */
    SOCIAL_START_IP("social:ip", Duration.ofMinutes(1), 30),
    /** P-38: apenas falhas contam. */
    SETUP_FAILURE_IP("setup:ip", Duration.ofMinutes(15), 5),
    /** P-39 */
    REAUTH_SESSION("reauth:sid", Duration.ofMinutes(15), 10),
    /** P-45 */
    INTROSPECT_API("introspect:api", Duration.ofMinutes(1), 600);

    private final String prefix;
    private final Duration window;
    private final int limit;

    RateRule(String prefix, Duration window, int limit) {
        this.prefix = prefix;
        this.window = window;
        this.limit = limit;
    }

    public String prefix() {
        return prefix;
    }

    public Duration window() {
        return window;
    }

    public int limit() {
        return limit;
    }

    public String key(String subject) {
        return prefix + ":" + subject;
    }
}
