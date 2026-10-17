package com.eridani.achernar.identity.shared.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Validação fail-fast dos parâmetros (PV-01…PV-06, deployment-operations §5).
 * Em {@code prod} qualquer violação impede a subida; em {@code dev} gera aviso.
 */
@Component
public class PropertiesValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesValidator.class);

    private final AchernarProperties props;
    private final Environment env;

    public PropertiesValidator(AchernarProperties props, Environment env) {
        this.props = props;
        this.env = env;
    }

    @PostConstruct
    void validateOnStartup() {
        boolean prod = env.acceptsProfiles(Profiles.of("prod"));
        List<String> problems = validate(props, prod);
        if (problems.isEmpty()) {
            LOG.info("Configuração efetiva: issuer={} ui={} socialGoogle={} socialFacebook={} smtpHost={} accessTtl={} refreshTtl={}",
                    props.issuer(), props.uiBaseUrl(), props.social().googleConfigured(),
                    props.social().facebookConfigured(), env.getProperty("spring.mail.host"),
                    props.token().accessTtl(), props.session().refreshTtl());
            return;
        }
        if (prod) {
            throw new IllegalStateException("Configuração inválida para produção: " + String.join("; ", problems));
        }
        problems.forEach(p -> LOG.warn("Configuração (dev) com aviso: {}", p));
    }

    /** Lógica pura, testável sem contexto Spring. */
    public static List<String> validate(AchernarProperties p, boolean prod) {
        List<String> out = new ArrayList<>();
        between(out, "ACHERNAR_TOKEN_ACCESS_TTL", p.token().accessTtl(), Duration.ofMinutes(5), Duration.ofMinutes(15));
        between(out, "ACHERNAR_SESSION_REFRESH_TTL", p.session().refreshTtl(), Duration.ofHours(1), Duration.ofDays(30));
        between(out, "ACHERNAR_SESSION_MAX_LIFETIME", p.session().maxLifetime(), p.session().refreshTtl(), Duration.ofDays(90));
        between(out, "ACHERNAR_REFRESH_REUSE_GRACE", p.refresh().reuseGrace(), Duration.ZERO, Duration.ofSeconds(30));
        between(out, "ACHERNAR_CLOCK_SKEW", p.clockSkew(), Duration.ZERO, Duration.ofSeconds(120));
        between(out, "ACHERNAR_REAUTH_WINDOW", p.session().reauthWindow(), Duration.ofMinutes(1), Duration.ofMinutes(15));
        between(out, "ACHERNAR_KEY_ROTATION_INTERVAL", p.key().rotationInterval(), Duration.ofDays(30), Duration.ofDays(365));
        between(out, "ACHERNAR_KEY_PUBLISH_LEAD", p.key().publishLead(), Duration.ofHours(1), Duration.ofDays(7));
        between(out, "ACHERNAR_JWKS_CACHE_MAX_AGE", p.key().jwksCacheMaxAge(), Duration.ofMinutes(1), Duration.ofHours(1));
        between(out, "ACHERNAR_SOCIAL_EXCHANGE_CODE_TTL", p.social().exchangeCodeTtl(), Duration.ofSeconds(10), Duration.ofSeconds(120));
        between(out, "ACHERNAR_SOCIAL_TRANSACTION_TTL", p.social().transactionTtl(), Duration.ofMinutes(2), Duration.ofMinutes(15));
        between(out, "ACHERNAR_VERIFY_EMAIL_TTL", p.oneTime().verifyEmailTtl(), Duration.ofHours(1), Duration.ofDays(7));
        between(out, "ACHERNAR_PASSWORD_RESET_TTL", p.oneTime().passwordResetTtl(), Duration.ofMinutes(10), Duration.ofHours(2));
        between(out, "ACHERNAR_EMAIL_CHANGE_TTL", p.oneTime().emailChangeTtl(), Duration.ofMinutes(10), Duration.ofHours(24));
        if (p.session().maxActive() < 1 || p.session().maxActive() > 50) {
            out.add("ACHERNAR_SESSION_MAX_ACTIVE fora de 1–50");
        }
        if (p.password().minLength() < 12 || p.password().minLength() > 64) {
            out.add("ACHERNAR_PASSWORD_MIN_LENGTH fora de 12–64");
        }
        if (p.password().maxLength() < 64 || p.password().maxLength() > 256) {
            out.add("ACHERNAR_PASSWORD_MAX_LENGTH fora de 64–256");
        }
        if (p.password().argon2MemoryKib() < 19456 || p.password().argon2Iterations() < 2) {
            out.add("Argon2id abaixo do mínimo (memória >= 19456 KiB, iterações >= 2)");
        }
        if (p.key().rsaBits() != 3072 && p.key().rsaBits() != 4096) {
            out.add("ACHERNAR_KEY_RSA_BITS deve ser 3072 ou 4096");
        }
        // PV-03: tokens válidos não podem perder a chave
        Duration needed = p.token().accessTtl().plus(p.clockSkew()).plus(p.key().jwksCacheMaxAge());
        if (p.key().retiredRetention().compareTo(needed) < 0) {
            out.add("PV-03: ACHERNAR_KEY_RETIRED_RETENTION deve ser >= TTL do access + skew + cache do JWKS");
        }
        // PV-04 está embutido na faixa de ACHERNAR_SESSION_MAX_LIFETIME acima.
        // PV-06: P-11 >= 2 x P-14
        if (p.key().publishLead().compareTo(p.key().jwksCacheMaxAge().multipliedBy(2)) < 0) {
            out.add("PV-06: ACHERNAR_KEY_PUBLISH_LEAD deve ser >= 2 x ACHERNAR_JWKS_CACHE_MAX_AGE");
        }
        if (p.issuer() == null || p.issuer().isBlank()) {
            out.add("ACHERNAR_ISSUER é obrigatório");
        } else if (prod && !p.issuer().startsWith("https://")) {
            out.add("PV-02: ACHERNAR_ISSUER deve ser https:// em produção");
        } else if (!prod && !(p.issuer().startsWith("https://") || p.issuer().startsWith("http://localhost")
                || p.issuer().startsWith("http://127.0.0.1"))) {
            out.add("PV-02: em dev o issuer aceita https:// ou http://localhost");
        }
        if (p.uiAudience() == null || p.uiAudience().isBlank()) {
            out.add("ACHERNAR_UI_AUDIENCE é obrigatório");
        }
        if (prod) {
            if (!p.cookie().secure()) {
                out.add("ACHERNAR_COOKIE_SECURE deve ser true em produção");
            }
            if (!masterKeyOk(p.masterKey())) {
                out.add("ACHERNAR_MASTER_KEY ausente ou menor que 32 bytes (base64)");
            }
            if (p.devInsecureKey()) {
                out.add("ACHERNAR_DEV_INSECURE_KEY não pode ser usado em produção");
            }
        } else if (!masterKeyOk(p.masterKey()) && !p.devInsecureKey()) {
            out.add("PV-05: ACHERNAR_MASTER_KEY ausente; defina a chave ou ACHERNAR_DEV_INSECURE_KEY=true (apenas dev)");
        }
        return out;
    }

    static boolean masterKeyOk(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(key.trim()).length >= 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void between(List<String> out, String name, Duration v, Duration min, Duration max) {
        if (v == null || v.compareTo(min) < 0 || v.compareTo(max) > 0) {
            out.add(name + " fora do intervalo " + min + "–" + max);
        }
    }
}
