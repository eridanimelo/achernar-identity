package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.credential.PasswordCredentialRepository;
import com.eridani.achernar.identity.credential.PasswordHasher;
import com.eridani.achernar.identity.credential.PasswordPolicy;
import com.eridani.achernar.identity.mail.ActionLinks;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.FieldError;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.Ips;
import com.eridani.achernar.identity.shared.web.LocaleSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Cadastro por e-mail/senha, verificação e reenvio (UC-010, UC-011, UC-012). */
@Service
public class RegistrationService {

    static final int MAX_PASSWORD_INPUT = 1024;

    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final OneTimeTokenService tokens;
    private final MailService mail;
    private final ActionLinks links;
    private final PasswordPolicy policy;
    private final PasswordHasher hasher;
    private final RateLimiter limiter;
    private final SecurityEventRecorder events;
    private final Clock clock;
    private final AchernarProperties props;
    private final TransactionTemplate tx;

    public RegistrationService(AccountRepository accounts, PasswordCredentialRepository credentials,
                               OneTimeTokenService tokens, MailService mail, ActionLinks links,
                               PasswordPolicy policy, PasswordHasher hasher, RateLimiter limiter,
                               SecurityEventRecorder events, Clock clock, AchernarProperties props,
                               TransactionTemplate tx) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.tokens = tokens;
        this.mail = mail;
        this.links = links;
        this.policy = policy;
        this.hasher = hasher;
        this.limiter = limiter;
        this.events = events;
        this.clock = clock;
        this.props = props;
        this.tx = tx;
    }

    // ------------------------------------------------------------------ UC-010
    public void register(ClientContext client, String email, String password, String bodyLocale,
                         String acceptLanguage) {
        Application app = client.application();
        if (!app.allowRegistration()) {
            throw new ApiException(ErrorCode.REGISTRATION_DISABLED);
        }
        EmailAddress.Normalized parsed = parseEmail(email);
        if (password == null || password.isEmpty()) {
            throw ApiException.validation("password", "REQUIRED");
        }
        if (password.length() > MAX_PASSWORD_INPUT) {
            throw new ApiException(ErrorCode.PASSWORD_POLICY_VIOLATION, List.of(FieldError.of("password", "TOO_LONG")));
        }
        List<FieldError> violations = policy.check(password, parsed.normalized());
        if (!violations.isEmpty()) {
            throw new ApiException(ErrorCode.PASSWORD_POLICY_VIOLATION, violations);
        }
        limiter.check(RateRule.REGISTER_IP, Ips.subject());
        if (!limiter.hit(RateRule.REGISTER_EMAIL, parsed.normalized()).allowed()) {
            return; // 202 silencioso (P-32)
        }
        String hash = hasher.hash(password); // sempre calcula: tempo equivalente com e-mail existente (D-13)
        String locale = chooseLocale(bodyLocale, acceptLanguage);
        tx.executeWithoutResult(status -> persist(app, parsed, hash, locale, bodyLocale, acceptLanguage));
    }

    private void persist(Application app, EmailAddress.Normalized parsed, String hash, String locale,
                         String bodyLocale, String acceptLanguage) {
        Instant now = clock.instant();
        Account fresh = new Account(UUID.randomUUID(), parsed.display(), parsed.normalized(),
                AccountStatus.PENDING_VERIFICATION, AccountRole.USER, null, locale, null, null, null, now, now, 0);
        if (accounts.insertIfAbsent(fresh)) {
            credentials.upsert(fresh.id(), hash, now);
            sendVerification(fresh, app);
            events.record(AuditEvent.of(EventType.ACCOUNT_REGISTERED).account(fresh.id()).client(app.clientId())
                    .detail("locale", locale));
            return;
        }
        Account existing = accounts.findByEmail(parsed.normalized()).orElse(null);
        if (existing == null) {
            return;
        }
        events.record(AuditEvent.of(EventType.REGISTRATION_DUPLICATE_ATTEMPT).account(existing.id())
                .client(app.clientId()));
        if (limiter.hit(RateRule.DUPLICATE_NOTICE_EMAIL, existing.emailNormalized()).allowed()) {
            String noticeLocale = LocaleSupport.isSupported(bodyLocale) ? bodyLocale
                    : LocaleSupport.resolve(acceptLanguage, props.defaultLocale());
            Map<String, String> vars = new HashMap<>();
            vars.put("app_name", app.name());
            vars.put("action_url", links.uiSignIn());
            vars.put("action_url2", links.uiForgotPassword());
            mail.send(MailTemplate.TPL_05, noticeLocale, existing.emailDisplay(), existing.id(), app.id(), vars);
        }
    }

    /** Emite token de verificação e enfileira TPL-01 (transação ativa exigida). */
    public void sendVerification(Account account, Application app) {
        Duration ttl = props.oneTime().verifyEmailTtl();
        OneTimeTokenService.Issued issued = tokens.issue(account.id(), TokenPurpose.EMAIL_VERIFICATION, app.id(),
                null, null, ttl);
        Map<String, String> vars = new HashMap<>();
        vars.put("app_name", app.name());
        vars.put("action_url", links.verifyEmail(app.verifyEmailUrl(), issued.token()));
        vars.put("expires_in", MailService.humanDuration(ttl, account.locale()));
        mail.send(MailTemplate.TPL_01, account.locale(), account.emailDisplay(), account.id(), app.id(), vars);
    }

    // ------------------------------------------------------------------ UC-011
    public void verifyEmail(ClientContext client, String token) {
        limiter.checkAll(Ips.subject(), List.of(RateRule.AUTH_IP_MINUTE, RateRule.AUTH_IP_HOUR));
        if (token == null || token.isBlank()) {
            throw ApiException.validation("token", "REQUIRED");
        }
        tx.executeWithoutResult(status -> {
            OneTimeTokenService.Consumed c = tokens.consume(token, TokenPurpose.EMAIL_VERIFICATION)
                    .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED));
            Instant now = clock.instant();
            if (accounts.activate(c.accountId(), now)) {
                events.record(AuditEvent.of(EventType.EMAIL_VERIFIED).account(c.accountId())
                        .client(client.application().clientId()));
            }
        });
    }

    // ------------------------------------------------------------------ UC-012
    public void resend(ClientContext client, String email) {
        EmailAddress.Normalized parsed = parseEmail(email);
        limiter.check(RateRule.RESEND_IP, Ips.subject());
        boolean minuteOk = limiter.hit(RateRule.RESEND_EMAIL_MINUTE, parsed.normalized()).allowed();
        boolean hourOk = limiter.hit(RateRule.RESEND_EMAIL_HOUR, parsed.normalized()).allowed();
        if (!minuteOk || !hourOk) {
            return; // 202 silencioso (P-34)
        }
        Application app = client.application();
        tx.executeWithoutResult(status -> {
            Account a = accounts.findByEmail(parsed.normalized()).orElse(null);
            if (a != null && a.status() == AccountStatus.PENDING_VERIFICATION && !a.anonymized()) {
                sendVerification(a, app);
                events.record(AuditEvent.of(EventType.EMAIL_VERIFICATION_RESENT).account(a.id())
                        .client(app.clientId()));
            }
        });
    }

    // ------------------------------------------------------------------ util
    static EmailAddress.Normalized parseEmail(String email) {
        return EmailAddress.parse(email).orElseThrow(() -> ApiException.validation("email", "INVALID_FORMAT"));
    }

    String chooseLocale(String bodyLocale, String acceptLanguage) {
        if (LocaleSupport.isSupported(bodyLocale)) {
            return bodyLocale;
        }
        return LocaleSupport.resolve(acceptLanguage, props.defaultLocale());
    }
}
