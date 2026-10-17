package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.crypto.TokenGenerator;
import com.eridani.achernar.identity.shared.db.PageResult;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.FieldError;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de aplicações (UC-060…063, UC-068). */
@Service
public class ApplicationService {

    public static final int MAX_APPLICATIONS = 100;
    public static final int MAX_ACTIVE_SECRETS = 2;

    public record Created(Application application, String clientSecret) {
    }

    public record SecretCreated(UUID secretId, String clientSecret, Instant createdAt) {
    }

    private final ApplicationRepository apps;
    private final ResourceApiRepository apis;
    private final SessionService sessions;
    private final SecurityEventRecorder events;
    private final Clock clock;
    private final AchernarProperties props;
    private final Environment env;

    public ApplicationService(ApplicationRepository apps, ResourceApiRepository apis, SessionService sessions,
                              SecurityEventRecorder events, Clock clock, AchernarProperties props, Environment env) {
        this.apps = apps;
        this.apis = apis;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
        this.props = props;
        this.env = env;
    }

    boolean allowLocalHttp() {
        return !env.acceptsProfiles(Profiles.of("prod"));
    }

    // ------------------------------------------------------------------ consulta
    @Transactional(readOnly = true)
    public Application get(UUID id) {
        return apps.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<Application> findByClientId(String clientId) {
        return apps.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public PageResult<ApplicationRepository.Summary> list(String q, String status, Integer page, Integer size,
                                                          String sort) {
        return apps.list(q, status, PageResult.clampPage(page), PageResult.clampSize(size), sort);
    }

    @Transactional(readOnly = true)
    public List<ApplicationRepository.SecretRow> secrets(UUID applicationId) {
        return apps.listSecrets(applicationId);
    }

    // ------------------------------------------------------------------ cadastro
    @Transactional
    public Created create(ApplicationInput in, UUID actor) {
        ApplicationValidator.Result v = ApplicationValidator.validate(in, true, allowLocalHttp(),
                props.social().googleConfigured(), props.social().facebookConfigured());
        List<FieldError> errors = new ArrayList<>(v.errors());
        List<AudienceGrant> grants = resolveAudiences(in.audiences(), errors);
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors);
        }
        if (apps.count() >= MAX_APPLICATIONS) {
            throw new ApiException(ErrorCode.CONFLICT, "LIMIT_REACHED");
        }
        if (apps.findByClientId(in.clientId()).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "CLIENT_ID_EXISTS");
        }
        Instant now = clock.instant();
        Application a = new Application(UUID.randomUUID(), in.clientId(), in.name().strip(), blankToNull(in.description()),
                ClientType.valueOf(in.type()), RegistryStatus.ACTIVE, in.allowRegistration(), in.identityApiAccess(),
                in.googleEnabled(), in.facebookEnabled(), in.accessTokenTtlSeconds(), blankToNull(in.verifyEmailUrl()),
                blankToNull(in.resetPasswordUrl()), blankToNull(in.confirmEmailChangeUrl()), false, now, now, 0,
                v.origins(), v.redirectUris(), grants);
        try {
            apps.insert(a);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorCode.CONFLICT, "CLIENT_ID_EXISTS");
        }
        String secret = null;
        if (a.type() == ClientType.CONFIDENTIAL) {
            secret = newSecret(a.id(), now);
        }
        events.record(AuditEvent.of(EventType.APPLICATION_CREATED).actor(actor).client(a.clientId())
                .detail("client_id", a.clientId()));
        return new Created(apps.findById(a.id()).orElseThrow(), secret);
    }

    // ------------------------------------------------------------------ alteração
    @Transactional
    public Application update(UUID id, ApplicationInput in, long expectedVersion, UUID actor) {
        Application current = apps.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (current.system() && (!"ACTIVE".equals(in.status()) || !in.identityApiAccess())) {
            throw new ApiException(ErrorCode.CONFLICT, "SYSTEM_APPLICATION_PROTECTED");
        }
        ApplicationValidator.Result v = ApplicationValidator.validate(in, false, allowLocalHttp(),
                props.social().googleConfigured(), props.social().facebookConfigured());
        List<FieldError> errors = new ArrayList<>(v.errors());
        List<AudienceGrant> grants = resolveAudiences(in.audiences(), errors);
        if (current.type() == ClientType.BROWSER && v.origins().isEmpty() && errors.stream().noneMatch(e -> "origins".equals(e.field()))) {
            errors.add(FieldError.of("origins", "REQUIRED"));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors);
        }
        Instant now = clock.instant();
        RegistryStatus newStatus = RegistryStatus.valueOf(in.status());
        Application updated = new Application(current.id(), current.clientId(), in.name().strip(),
                blankToNull(in.description()), current.type(), newStatus, in.allowRegistration(),
                in.identityApiAccess(), in.googleEnabled(), in.facebookEnabled(), in.accessTokenTtlSeconds(),
                blankToNull(in.verifyEmailUrl()), blankToNull(in.resetPasswordUrl()),
                blankToNull(in.confirmEmailChangeUrl()), current.system(), current.createdAt(), now,
                current.version(), v.origins(), v.redirectUris(), grants);
        if (!apps.update(updated, expectedVersion)) {
            throw new ApiException(ErrorCode.CONFLICT, "VERSION_MISMATCH");
        }
        EventType type = EventType.APPLICATION_UPDATED;
        if (current.status() == RegistryStatus.ACTIVE && newStatus == RegistryStatus.DISABLED) {
            type = EventType.APPLICATION_DISABLED;
            sessions.revokeAllForApplication(current.id(), "APP_DISABLED");
        } else if (current.status() == RegistryStatus.DISABLED && newStatus == RegistryStatus.ACTIVE) {
            type = EventType.APPLICATION_ENABLED;
        }
        events.record(AuditEvent.of(type).actor(actor).client(current.clientId())
                .detail("client_id", current.clientId()).detail("changed", changed(current, updated)));
        return apps.findById(id).orElseThrow();
    }

    // ------------------------------------------------------------------ segredos
    @Transactional
    public SecretCreated createSecret(UUID applicationId, UUID actor) {
        Application a = apps.findById(applicationId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (a.type() != ClientType.CONFIDENTIAL) {
            throw ApiException.validation("type", "NOT_CONFIDENTIAL");
        }
        if (apps.countActiveSecrets(a.id()) >= MAX_ACTIVE_SECRETS) {
            throw new ApiException(ErrorCode.CONFLICT, "MAX_ACTIVE_SECRETS");
        }
        Instant now = clock.instant();
        UUID secretId = UUID.randomUUID();
        String secret = TokenGenerator.newToken();
        apps.insertSecret(secretId, a.id(), Sha256.hex(secret), now);
        events.record(AuditEvent.of(EventType.APPLICATION_SECRET_ROTATED).actor(actor).client(a.clientId())
                .detail("client_id", a.clientId()));
        return new SecretCreated(secretId, secret, now);
    }

    @Transactional
    public void revokeSecret(UUID applicationId, UUID secretId, UUID actor) {
        Application a = apps.findById(applicationId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!apps.secretExists(a.id(), secretId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        if (!apps.secretIsActive(a.id(), secretId)) {
            return; // idempotente
        }
        if (a.type() == ClientType.CONFIDENTIAL && apps.countActiveSecrets(a.id()) <= 1) {
            throw new ApiException(ErrorCode.CONFLICT, "LAST_ACTIVE_SECRET");
        }
        apps.revokeSecret(a.id(), secretId, clock.instant());
        events.record(AuditEvent.of(EventType.APPLICATION_SECRET_ROTATED).actor(actor).client(a.clientId())
                .detail("client_id", a.clientId()));
    }

    // ------------------------------------------------------------------ auxiliares
    private String newSecret(UUID applicationId, Instant now) {
        String secret = TokenGenerator.newToken();
        apps.insertSecret(UUID.randomUUID(), applicationId, Sha256.hex(secret), now);
        return secret;
    }

    private List<AudienceGrant> resolveAudiences(List<ApplicationInput.AudienceInput> inputs, List<FieldError> errors) {
        List<AudienceGrant> out = new ArrayList<>();
        if (inputs == null) {
            return out;
        }
        for (int i = 0; i < inputs.size(); i++) {
            ApplicationInput.AudienceInput a = inputs.get(i);
            if (a == null || a.audience() == null) {
                continue;
            }
            Optional<ResourceApi> api = apis.findByAudience(a.audience());
            if (api.isEmpty()) {
                errors.add(FieldError.of("audiences[" + i + "].audience", "UNKNOWN_API"));
            } else {
                out.add(new AudienceGrant(api.get().id(), api.get().audience(), a.isDefault(), api.get().active()));
            }
        }
        return out;
    }

    static String changed(Application a, Application b) {
        List<String> c = new ArrayList<>();
        if (!a.name().equals(b.name())) c.add("name");
        if (!java.util.Objects.equals(a.description(), b.description())) c.add("description");
        if (a.status() != b.status()) c.add("status");
        if (!a.origins().equals(b.origins())) c.add("origins");
        if (!a.redirectUris().equals(b.redirectUris())) c.add("redirect_uris");
        if (a.allowRegistration() != b.allowRegistration()) c.add("allow_registration");
        if (a.identityApiAccess() != b.identityApiAccess()) c.add("identity_api_access");
        if (a.googleEnabled() != b.googleEnabled() || a.facebookEnabled() != b.facebookEnabled()) c.add("providers");
        if (!java.util.Objects.equals(a.accessTokenTtlSeconds(), b.accessTokenTtlSeconds())) c.add("access_token_ttl");
        if (!java.util.Objects.equals(a.verifyEmailUrl(), b.verifyEmailUrl())
                || !java.util.Objects.equals(a.resetPasswordUrl(), b.resetPasswordUrl())
                || !java.util.Objects.equals(a.confirmEmailChangeUrl(), b.confirmEmailChangeUrl())) c.add("action_urls");
        if (!a.audiences().equals(b.audiences())) c.add("audiences");
        return String.join(",", c);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
