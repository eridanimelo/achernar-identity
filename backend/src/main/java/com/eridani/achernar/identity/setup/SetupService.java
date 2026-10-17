package com.eridani.achernar.identity.setup;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.account.AccountRole;
import com.eridani.achernar.identity.account.AccountStatus;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.credential.PasswordCredentialRepository;
import com.eridani.achernar.identity.credential.PasswordHasher;
import com.eridani.achernar.identity.credential.PasswordPolicy;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.ConstantTime;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.FieldError;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.Ips;
import com.eridani.achernar.identity.shared.web.LocaleSupport;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** FT-01: estado do setup e criação do primeiro administrador (RN-SET-01…07). */
@Service
public class SetupService {

    public record Status(boolean setupRequired, boolean setupAvailable) {
    }

    public record Created(UUID accountId, String email) {
    }

    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final PasswordPolicy policy;
    private final PasswordHasher hasher;
    private final RateLimiter limiter;
    private final SecurityEventRecorder events;
    private final AchernarProperties props;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate tx;

    public SetupService(AccountRepository accounts, PasswordCredentialRepository credentials, PasswordPolicy policy,
                        PasswordHasher hasher, RateLimiter limiter, SecurityEventRecorder events,
                        AchernarProperties props, JdbcTemplate jdbc, Clock clock, TransactionTemplate tx) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.policy = policy;
        this.hasher = hasher;
        this.limiter = limiter;
        this.events = events;
        this.props = props;
        this.jdbc = jdbc;
        this.clock = clock;
        this.tx = tx;
    }

    private boolean tokenConfigured() {
        return props.bootstrapToken() != null && !props.bootstrapToken().isBlank();
    }

    public Status status() {
        limiter.requireNotExceeded(RateRule.SETUP_FAILURE_IP, Ips.subject());
        boolean required = !accounts.hasActiveAdmin();
        return new Status(required, required && tokenConfigured());
    }

    public Created create(String bootstrapToken, String email, String password, String locale) {
        String ip = Ips.subject();
        limiter.requireNotExceeded(RateRule.SETUP_FAILURE_IP, ip); // apenas falhas contam (P-38)
        if (accounts.hasActiveAdmin() || !tokenConfigured()) {
            throw new ApiException(ErrorCode.SETUP_NOT_AVAILABLE);
        }
        if (bootstrapToken == null || bootstrapToken.length() > 256
                || !ConstantTime.equals(Sha256.hex(bootstrapToken), Sha256.hex(props.bootstrapToken()))) {
            limiter.hit(RateRule.SETUP_FAILURE_IP, ip);
            events.recordIndependent(EventType.SETUP_ATTEMPT_FAILED, null, null, Map.of());
            throw new ApiException(ErrorCode.SETUP_TOKEN_INVALID);
        }
        EmailAddress.Normalized parsed = EmailAddress.parse(email)
                .orElseThrow(() -> ApiException.validation("email", "INVALID_FORMAT"));
        if (password == null || password.isEmpty() || password.length() > 1024) {
            throw ApiException.validation("password", "REQUIRED");
        }
        List<FieldError> violations = policy.check(password, parsed.normalized());
        if (!violations.isEmpty()) {
            throw new ApiException(ErrorCode.PASSWORD_POLICY_VIOLATION, violations);
        }
        if (locale != null && !LocaleSupport.isSupported(locale)) {
            throw ApiException.validation("locale", "INVALID");
        }
        String chosenLocale = locale != null ? locale : props.defaultLocale();
        String hash = hasher.hash(password);
        return tx.execute(status -> {
            Db.advisoryXactLock(jdbc, "setup");
            if (accounts.hasActiveAdmin()) {
                throw new ApiException(ErrorCode.SETUP_NOT_AVAILABLE);
            }
            Instant now = clock.instant();
            Account admin = new Account(UUID.randomUUID(), parsed.display(), parsed.normalized(),
                    AccountStatus.ACTIVE, AccountRole.ADMIN, now, chosenLocale, null, null, null, now, now, 0);
            if (!accounts.insertAdmin(admin)) {
                throw new ApiException(ErrorCode.CONFLICT);
            }
            credentials.upsert(admin.id(), hash, now);
            events.record(AuditEvent.of(EventType.BOOTSTRAP_ADMIN_CREATED).account(admin.id()));
            return new Created(admin.id(), admin.emailDisplay());
        });
    }
}
