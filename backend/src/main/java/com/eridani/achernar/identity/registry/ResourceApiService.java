package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.ConstantTime;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** APIs consumidoras e suas credenciais de introspecção (UC-064…067). */
@Service
public class ResourceApiService {

    public static final int MAX_APIS = 100;
    public static final int MAX_ACTIVE_CREDENTIALS = 2;
    private static final Pattern AUDIENCE = Pattern.compile("^[^\\s]{1,200}$");

    public record Created(ResourceApi api, UUID credentialId, String secret) {
    }

    public record CredentialCreated(UUID credentialId, String secret, Instant createdAt) {
    }

    private final ResourceApiRepository apis;
    private final SecurityEventRecorder events;
    private final Clock clock;
    private final AchernarProperties props;

    public ResourceApiService(ResourceApiRepository apis, SecurityEventRecorder events, Clock clock,
                              AchernarProperties props) {
        this.apis = apis;
        this.events = events;
        this.clock = clock;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public ResourceApi get(UUID id) {
        return apis.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PageResult<ResourceApi> list(String q, String status, Integer page, Integer size, String sort) {
        return apis.list(q, status, PageResult.clampPage(page), PageResult.clampSize(size), sort);
    }

    @Transactional(readOnly = true)
    public List<ResourceApiRepository.CredentialRow> credentials(UUID apiId) {
        return apis.listCredentials(apiId);
    }

    @Transactional
    public Created create(String audience, String name, String description, UUID actor) {
        List<FieldError> errors = new ArrayList<>();
        String aud = audience == null ? null : audience.strip();
        if (aud == null || !AUDIENCE.matcher(aud).matches()) {
            errors.add(FieldError.of("audience", "INVALID_FORMAT"));
        } else if (aud.equals(props.uiAudience())) {
            errors.add(FieldError.of("audience", "RESERVED"));
        }
        validateNameDescription(name, description, errors);
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors);
        }
        if (apis.count() >= MAX_APIS) {
            throw new ApiException(ErrorCode.CONFLICT, "LIMIT_REACHED");
        }
        if (apis.findByAudience(aud).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "AUDIENCE_EXISTS");
        }
        Instant now = clock.instant();
        ResourceApi api = new ResourceApi(UUID.randomUUID(), aud, name.strip(), blankToNull(description),
                RegistryStatus.ACTIVE, now, now, 0);
        try {
            apis.insert(api);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorCode.CONFLICT, "AUDIENCE_EXISTS");
        }
        UUID credentialId = UUID.randomUUID();
        String secret = TokenGenerator.newToken();
        apis.insertCredential(credentialId, api.id(), Sha256.hex(secret), now);
        events.record(AuditEvent.of(EventType.API_CREATED).actor(actor).detail("audience", aud));
        return new Created(apis.findById(api.id()).orElseThrow(), credentialId, secret);
    }

    /** PATCH: {@code name}, {@code description}, {@code status}, {@code version}; campos desconhecidos são erro. */
    @Transactional
    public ResourceApi update(UUID id, Map<String, Object> patch, UUID actor) {
        List<FieldError> errors = new ArrayList<>();
        for (String k : patch.keySet()) {
            if (!List.of("name", "description", "status", "version").contains(k)) {
                errors.add(FieldError.of(k, "UNKNOWN_FIELD"));
            }
        }
        if (!(patch.get("version") instanceof Number)) {
            errors.add(FieldError.of("version", "REQUIRED"));
        }
        ResourceApi cur = apis.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String name = cur.name();
        String description = cur.description();
        RegistryStatus status = cur.status();
        if (patch.containsKey("name")) {
            if (patch.get("name") instanceof String s) {
                name = s;
            } else {
                errors.add(FieldError.of("name", "INVALID_TYPE"));
            }
        }
        if (patch.containsKey("description")) {
            Object d = patch.get("description");
            if (d == null || d instanceof String) {
                description = (String) d;
            } else {
                errors.add(FieldError.of("description", "INVALID_TYPE"));
            }
        }
        if (patch.containsKey("status")) {
            Object s = patch.get("status");
            if ("ACTIVE".equals(s) || "DISABLED".equals(s)) {
                status = RegistryStatus.valueOf((String) s);
            } else {
                errors.add(FieldError.of("status", "INVALID"));
            }
        }
        validateNameDescription(name, description, errors);
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors);
        }
        long version = ((Number) patch.get("version")).longValue();
        ResourceApi updated = new ResourceApi(cur.id(), cur.audience(), name.strip(), blankToNull(description), status,
                cur.createdAt(), clock.instant(), cur.version());
        if (!apis.update(updated, version)) {
            throw new ApiException(ErrorCode.CONFLICT, "VERSION_MISMATCH");
        }
        EventType type = EventType.API_UPDATED;
        if (cur.status() == RegistryStatus.ACTIVE && status == RegistryStatus.DISABLED) {
            type = EventType.API_DISABLED;
        } else if (cur.status() == RegistryStatus.DISABLED && status == RegistryStatus.ACTIVE) {
            type = EventType.API_ENABLED;
        }
        List<String> changed = new ArrayList<>();
        if (!cur.name().equals(updated.name())) changed.add("name");
        if (!Objects.equals(cur.description(), updated.description())) changed.add("description");
        if (cur.status() != updated.status()) changed.add("status");
        events.record(AuditEvent.of(type).actor(actor).detail("audience", cur.audience())
                .detail("changed", String.join(",", changed)));
        return apis.findById(id).orElseThrow();
    }

    @Transactional
    public CredentialCreated createCredential(UUID apiId, UUID actor) {
        ResourceApi api = apis.findById(apiId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (apis.countActiveCredentials(apiId) >= MAX_ACTIVE_CREDENTIALS) {
            throw new ApiException(ErrorCode.CONFLICT, "MAX_ACTIVE_CREDENTIALS");
        }
        Instant now = clock.instant();
        UUID id = UUID.randomUUID();
        String secret = TokenGenerator.newToken();
        apis.insertCredential(id, apiId, Sha256.hex(secret), now);
        events.record(AuditEvent.of(EventType.API_CREDENTIAL_ROTATED).actor(actor).detail("audience", api.audience()));
        return new CredentialCreated(id, secret, now);
    }

    @Transactional
    public void revokeCredential(UUID apiId, UUID credentialId, UUID actor) {
        ResourceApi api = apis.findById(apiId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!apis.credentialExists(apiId, credentialId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        if (!apis.credentialIsActive(apiId, credentialId)) {
            return;
        }
        if (apis.countActiveCredentials(apiId) <= 1) {
            throw new ApiException(ErrorCode.CONFLICT, "LAST_ACTIVE_CREDENTIAL");
        }
        apis.revokeCredential(apiId, credentialId, clock.instant());
        events.record(AuditEvent.of(EventType.API_CREDENTIAL_ROTATED).actor(actor).detail("audience", api.audience()));
    }

    /** Autentica a API na introspecção (RN-APP-16). {@code INVALID_CLIENT} para qualquer falha. */
    @Transactional
    public ResourceApi authenticate(String audience, String secret) {
        ResourceApi api = apis.findByAudience(audience).orElse(null);
        if (api == null || !api.active()) {
            ConstantTime.equals(Sha256.hex(secret), "x");
            throw new ApiException(ErrorCode.INVALID_CLIENT);
        }
        String hash = Sha256.hex(secret);
        boolean ok = false;
        for (String stored : apis.activeCredentialHashes(api.id())) {
            ok |= ConstantTime.equals(stored, hash);
        }
        if (!ok) {
            throw new ApiException(ErrorCode.INVALID_CLIENT);
        }
        apis.touchCredential(api.id(), hash, clock.instant());
        return api;
    }

    private static void validateNameDescription(String name, String description, List<FieldError> errors) {
        if (name == null || name.isBlank()) {
            errors.add(FieldError.of("name", "REQUIRED"));
        } else if (name.strip().length() > 100) {
            errors.add(FieldError.of("name", "TOO_LONG"));
        }
        if (description != null && description.length() > 500) {
            errors.add(FieldError.of("description", "TOO_LONG"));
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
