package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.MasterKeyCipher;
import com.eridani.achernar.identity.shared.db.Db;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chaves de assinatura RS256 (D-04, D-25): geração, cifra, rotação, remoção e publicação (JWKS). */
@Service
public class SigningKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(SigningKeyService.class);
    private static final long SIGNER_CACHE_MS = 60_000;
    private static final long VERIFY_CACHE_MS = 15_000;
    static final String LOCK = "signing-keys";

    public record Signer(String kid, PrivateKey privateKey) {
    }

    public record RunResult(List<String> performed) {
    }

    private record CachedSigner(Signer signer, long loadedAtMs) {
    }

    private record CachedVerification(Map<String, RSAPublicKey> keys, List<String> jwks, long loadedAtMs) {
    }

    private final SigningKeyRepository repository;
    private final MasterKeyCipher cipher;
    private final SecurityEventRecorder events;
    private final SessionService sessions;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AchernarProperties props;

    private volatile CachedSigner signerCache;
    private volatile CachedVerification verificationCache;

    public SigningKeyService(SigningKeyRepository repository, MasterKeyCipher cipher, SecurityEventRecorder events,
                             SessionService sessions, JdbcTemplate jdbc, Clock clock, AchernarProperties props) {
        this.repository = repository;
        this.cipher = cipher;
        this.events = events;
        this.sessions = sessions;
        this.jdbc = jdbc;
        this.clock = clock;
        this.props = props;
    }

    // ------------------------------------------------------------------ uso (assinatura/verificação)
    /** Chave ativa para assinar (cache ≤ 60 s). */
    public Signer activeSigner() {
        CachedSigner c = signerCache;
        long now = System.currentTimeMillis();
        if (c != null && now - c.loadedAtMs() < SIGNER_CACHE_MS) {
            return c.signer();
        }
        SigningKey active = repository.findActive()
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "NO_ACTIVE_SIGNING_KEY"));
        Signer s = new Signer(active.kid(), decryptPrivate(active));
        signerCache = new CachedSigner(s, now);
        return s;
    }

    /** Chaves públicas para validar assinaturas (PUBLISHED ∪ ACTIVE ∪ RETIRED), cache curto. */
    public Map<String, RSAPublicKey> verificationKeys() {
        return verification().keys();
    }

    /** Documento JWKS (RFC 7517) com apenas a parte pública. */
    public String jwksJson() {
        StringBuilder sb = new StringBuilder("{\"keys\":[");
        sb.append(String.join(",", verification().jwks()));
        return sb.append("]}").toString();
    }

    public boolean hasActiveKey() {
        try {
            return repository.findActive().isPresent();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public Instant activeKeyActivatedAt() {
        return repository.findActive().map(SigningKey::activateAt).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SigningKey> list() {
        return repository.findAll();
    }

    private CachedVerification verification() {
        CachedVerification c = verificationCache;
        long now = System.currentTimeMillis();
        if (c != null && now - c.loadedAtMs() < VERIFY_CACHE_MS) {
            return c;
        }
        Map<String, RSAPublicKey> keys = new LinkedHashMap<>();
        List<String> jwks = new ArrayList<>();
        for (SigningKey k : repository.findPublishable()) {
            try {
                RSAKey jwk = RSAKey.parse(k.publicJwk());
                keys.put(k.kid(), jwk.toRSAPublicKey());
                jwks.add(jwk.toPublicJWK().toJSONString());
            } catch (ParseException | JOSEException e) {
                LOG.error("Chave pública inválida kid={}", k.kid());
            }
        }
        CachedVerification fresh = new CachedVerification(keys, jwks, now);
        verificationCache = fresh;
        return fresh;
    }

    void invalidateCaches() {
        signerCache = null;
        verificationCache = null;
    }

    // ------------------------------------------------------------------ ciclo de vida
    /** Garante uma chave ACTIVE na subida (primeira subida gera K0 e a ativa). */
    @Transactional
    public void ensureActiveKey() {
        Db.advisoryXactLock(jdbc, LOCK);
        if (repository.findActive().isEmpty()) {
            SigningKey k = generate(KeyStatus.ACTIVE, clock.instant());
            repository.insert(k);
            events.record(AuditEvent.of(EventType.SIGNING_KEY_ROTATED).detail("kid", k.kid()).detail("mode", "bootstrap"));
            LOG.info("Chave de assinatura inicial criada kid={}", k.kid());
        }
        invalidateCaches();
    }

    /** Executa o plano de rotação automática (job). Retorna vazio se outra instância já está executando. */
    @Transactional
    public RunResult runScheduled() {
        if (!Db.tryAdvisoryXactLock(jdbc, LOCK)) {
            return new RunResult(List.of());
        }
        Instant now = clock.instant();
        List<KeyRotationPlanner.KeyState> states = repository.findAll().stream()
                .filter(k -> k.status() != KeyStatus.REMOVED)
                .map(k -> new KeyRotationPlanner.KeyState(k.kid(), k.status(), k.publishAt(), k.activateAt(), k.retireAt()))
                .toList();
        List<KeyRotationPlanner.Action> actions = KeyRotationPlanner.plan(states, now,
                new KeyRotationPlanner.Params(props.key().rotationInterval(), props.key().publishLead(),
                        props.key().retiredRetention()));
        List<String> performed = new ArrayList<>();
        for (KeyRotationPlanner.Action a : actions) {
            switch (a.type()) {
                case BOOTSTRAP -> {
                    SigningKey k = generate(KeyStatus.ACTIVE, now);
                    repository.insert(k);
                    events.record(AuditEvent.of(EventType.SIGNING_KEY_ROTATED).detail("kid", k.kid()).detail("mode", "bootstrap"));
                    performed.add("BOOTSTRAP:" + k.kid());
                }
                case GENERATE_NEXT -> {
                    SigningKey k = generate(KeyStatus.PUBLISHED, now);
                    repository.insert(k);
                    performed.add("PUBLISHED:" + k.kid());
                }
                case PROMOTE -> {
                    repository.findActive().ifPresent(old -> repository.retire(old.kid(), now));
                    repository.activate(a.kid(), now);
                    events.record(AuditEvent.of(EventType.SIGNING_KEY_ROTATED).detail("kid", a.kid()).detail("mode", "automatic"));
                    performed.add("ACTIVE:" + a.kid());
                }
                case REMOVE_EXPIRED -> {
                    repository.remove(a.kid(), now, "EXPIRED");
                    events.record(AuditEvent.of(EventType.SIGNING_KEY_REMOVED).detail("kid", a.kid()).detail("emergency", false));
                    performed.add("REMOVED:" + a.kid());
                }
                default -> { }
            }
        }
        if (!performed.isEmpty()) {
            invalidateCaches();
        }
        return new RunResult(performed);
    }

    /** Rotação manual (UC-072): gera, ativa imediatamente e aposenta a atual. */
    @Transactional
    public SigningKey rotateNow(UUID actor) {
        Db.advisoryXactLock(jdbc, LOCK);
        Instant now = clock.instant();
        repository.findActive().ifPresent(old -> repository.retire(old.kid(), now));
        SigningKey k = generate(KeyStatus.ACTIVE, now);
        repository.insert(k);
        events.record(AuditEvent.of(EventType.SIGNING_KEY_ROTATED).actor(actor).detail("kid", k.kid())
                .detail("mode", "manual"));
        invalidateCaches();
        return k;
    }

    /** Remoção (UC-072); emergencial remove na hora, ativa substituta e opcionalmente revoga todas as sessões. */
    @Transactional
    public void remove(String kid, boolean emergency, boolean revokeAllSessions, UUID actor) {
        Db.advisoryXactLock(jdbc, LOCK);
        SigningKey key = repository.findByKid(kid).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (key.status() == KeyStatus.REMOVED) {
            return;
        }
        if (key.status() == KeyStatus.ACTIVE && !emergency) {
            throw new ApiException(ErrorCode.CONFLICT, "ACTIVE_KEY_REQUIRES_EMERGENCY");
        }
        Instant now = clock.instant();
        boolean wasActive = key.status() == KeyStatus.ACTIVE;
        repository.remove(kid, now, emergency ? "EMERGENCY" : "MANUAL");
        if (wasActive) {
            SigningKey replacement = generate(KeyStatus.ACTIVE, now);
            repository.insert(replacement);
            events.record(AuditEvent.of(EventType.SIGNING_KEY_ROTATED).actor(actor).detail("kid", replacement.kid())
                    .detail("mode", "emergency"));
        }
        events.record(AuditEvent.of(EventType.SIGNING_KEY_REMOVED).actor(actor).detail("kid", kid)
                .detail("emergency", emergency));
        if (emergency && revokeAllSessions) {
            sessions.revokeEverySession("KEY_COMPROMISE");
        }
        invalidateCaches();
    }

    @Transactional
    public int purgeRemoved(Instant cutoff, int batch) {
        return repository.purgeRemoved(cutoff, batch);
    }

    // ------------------------------------------------------------------ geração
    private SigningKey generate(KeyStatus status, Instant now) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(props.key().rsaBits());
            KeyPair pair = gen.generateKeyPair();
            RSAKey jwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).keyIDFromThumbprint().build();
            String kid = jwk.getKeyID();
            byte[] enc = cipher.encrypt(pair.getPrivate().getEncoded(), "signing-key:" + kid);
            Instant activateAt = status == KeyStatus.PUBLISHED ? now.plus(props.key().publishLead()) : now;
            return new SigningKey(UUID.randomUUID(), kid, jwk.toPublicJWK().toJSONString(), enc, status, now, now,
                    activateAt, null, null, null);
        } catch (java.security.GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("Falha ao gerar chave de assinatura", e);
        }
    }

    private PrivateKey decryptPrivate(SigningKey key) {
        try {
            byte[] pkcs8 = cipher.decrypt(key.privateKeyEnc(), "signing-key:" + key.kid());
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Chave privada ilegível kid=" + key.kid(), e);
        }
    }
}
