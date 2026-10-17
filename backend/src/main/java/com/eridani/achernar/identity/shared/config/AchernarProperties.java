package com.eridani.achernar.identity.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros configuráveis (parameters.md, P-xx). Os padrões vivem em {@code application.yml}
 * com as variáveis {@code ACHERNAR_*}. Validação de limites em {@link PropertiesValidator}.
 */
@ConfigurationProperties(prefix = "achernar")
public record AchernarProperties(
        String issuer,
        String uiBaseUrl,
        List<String> uiOrigins,
        String uiAudience,
        String defaultLocale,
        String masterKey,
        boolean devInsecureKey,
        String bootstrapToken,
        List<String> trustedProxies,
        Duration corsMaxAge,
        int maxBodyBytes,
        Duration clockSkew,
        Cookie cookie,
        Token token,
        Session session,
        Refresh refresh,
        Key key,
        Password password,
        OneTime oneTime,
        Social social,
        Mail mail,
        Retention retention,
        Jobs jobs) {

    public record Cookie(boolean secure, String domain) {
    }

    public record Token(Duration accessTtl) {
    }

    public record Session(Duration refreshTtl, Duration maxLifetime, int maxActive, Duration reauthWindow) {
    }

    public record Refresh(Duration reuseGrace) {
    }

    public record Key(Duration rotationInterval, Duration publishLead, Duration retiredRetention,
                      int rsaBits, Duration jwksCacheMaxAge) {
    }

    public record Password(int minLength, int maxLength, int argon2MemoryKib, int argon2Iterations,
                           int argon2Parallelism, int hashConcurrency) {
    }

    public record OneTime(Duration verifyEmailTtl, Duration passwordResetTtl, Duration emailChangeTtl) {
    }

    public record Social(Duration exchangeCodeTtl, Duration transactionTtl,
                         String googleClientId, String googleClientSecret,
                         String facebookAppId, String facebookAppSecret, String facebookGraphVersion,
                         Duration httpTimeout) {

        public boolean googleConfigured() {
            return notBlank(googleClientId) && notBlank(googleClientSecret);
        }

        public boolean facebookConfigured() {
            return notBlank(facebookAppId) && notBlank(facebookAppSecret);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    public record Mail(String from, String fromName, String replyTo, String supportContact,
                       int maxAttempts, Duration failedRetention, Duration workerInterval, int workerBatch) {
    }

    public record Retention(Duration unverifiedAccount, Duration oneTimeTokens, Duration sessions,
                            Duration securityEvents, Duration eventIp, Duration outboxSent,
                            Duration rateLimit, Duration retiredKeys, int batchSize) {
    }

    public record Jobs(String keyRotationCron, String retentionCron, String maintenanceCron) {
    }

    /** Remove barras finais para compor URLs de forma previsível. */
    public String issuerBase() {
        return stripTrailingSlash(issuer);
    }

    public String uiBase() {
        return stripTrailingSlash(uiBaseUrl);
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        String r = s.trim();
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }
}
