package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.account.AccountRole;
import com.eridani.achernar.identity.account.AccountStatus;
import com.eridani.achernar.identity.account.RegistrationService;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.auth.AudiencePolicy;
import com.eridani.achernar.identity.auth.TokenResponses;
import com.eridani.achernar.identity.mail.ActionLinks;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ApplicationRepository;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.session.Session;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.ConstantTime;
import com.eridani.achernar.identity.shared.crypto.MasterKeyCipher;
import com.eridani.achernar.identity.shared.crypto.Pkce;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import com.eridani.achernar.identity.shared.crypto.TokenGenerator;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.Ips;
import com.eridani.achernar.identity.shared.web.LocaleSupport;
import com.eridani.achernar.identity.token.JwtIssuer;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** FT-07: início (UC-050), retorno do provedor (UC-051) e troca do código (UC-052); FT-08 callbacks. */
@Service
public class SocialLoginService {

    private static final Logger LOG = LoggerFactory.getLogger(SocialLoginService.class);
    private static final String PKCE_PURPOSE = "social-pkce";
    static final int MAX_CLIENT_STATE = 500;

    public record StartParams(String clientId, String redirectUri, String state, String codeChallenge,
                              String codeChallengeMethod, String audiences, String intent) {
    }

    /** Redirecionamento 302 com {@code Set-Cookie} opcional. */
    public record Redirect(String location, String setCookie) {
    }

    private sealed interface Exchange permits ExchangeOk, ExchangeFail {
    }

    private record ExchangeOk(TokenResponses.Result result) implements Exchange {
    }

    private record ExchangeFail() implements Exchange {
    }

    private record Outcome(String code, String error) {
        static Outcome ok(String code) {
            return new Outcome(code, null);
        }

        static Outcome error(String error) {
            return new Outcome(null, error);
        }
    }

    private final Map<Provider, ProviderClient> clients = new EnumMap<>(Provider.class);
    private final ApplicationRepository apps;
    private final SocialTransactionRepository transactions;
    private final SocialExchangeCodeRepository codes;
    private final SocialLinkRepository links;
    private final AccountRepository accounts;
    private final RegistrationService registration;
    private final MailService mail;
    private final ActionLinks actionLinks;
    private final MasterKeyCipher cipher;
    private final RateLimiter limiter;
    private final SecurityEventRecorder events;
    private final SessionService sessions;
    private final AudiencePolicy audiencePolicy;
    private final JwtIssuer jwt;
    private final TokenResponses responses;
    private final SocialIntentCodec intents;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public SocialLoginService(List<ProviderClient> providerClients, ApplicationRepository apps,
                              SocialTransactionRepository transactions, SocialExchangeCodeRepository codes,
                              SocialLinkRepository links, AccountRepository accounts,
                              RegistrationService registration, MailService mail, ActionLinks actionLinks,
                              MasterKeyCipher cipher, RateLimiter limiter, SecurityEventRecorder events,
                              SessionService sessions, AudiencePolicy audiencePolicy, JwtIssuer jwt,
                              TokenResponses responses, SocialIntentCodec intents, AchernarProperties props,
                              Clock clock, TransactionTemplate tx) {
        providerClients.forEach(c -> this.clients.put(c.provider(), c));
        this.apps = apps;
        this.transactions = transactions;
        this.codes = codes;
        this.links = links;
        this.accounts = accounts;
        this.registration = registration;
        this.mail = mail;
        this.actionLinks = actionLinks;
        this.cipher = cipher;
        this.limiter = limiter;
        this.events = events;
        this.sessions = sessions;
        this.audiencePolicy = audiencePolicy;
        this.jwt = jwt;
        this.responses = responses;
        this.intents = intents;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    public String callbackUrl(Provider p) {
        return props.issuerBase() + "/auth/social/" + p.slug() + "/callback";
    }

    public boolean providerEnabled(Provider p, Application app) {
        boolean appFlag = p == Provider.GOOGLE ? app.googleEnabled() : app.facebookEnabled();
        return clients.get(p).configured() && appFlag;
    }

    public String bindingCookieName() {
        return (props.cookie().secure() ? "__Secure-" : "") + "achsoc";
    }

    // ------------------------------------------------------------------ UC-050
    public Redirect start(Provider provider, StartParams p) {
        limiter.check(RateRule.SOCIAL_START_IP, Ips.subject());
        ProviderClient client = clients.get(provider);
        Application app;
        SocialTransaction.Purpose purpose;
        String redirectUri;
        String clientState;
        String challenge = null;
        String audiences = null;
        UUID accountId = null;
        UUID sessionId = null;
        if (p.intent() != null && !p.intent().isBlank()) {
            SocialIntentCodec.Intent in = intents.decode(p.intent())
                    .orElseThrow(() -> new SocialPageException("INVALID_INTENT", 400));
            if (in.provider() != provider) {
                throw new SocialPageException("INVALID_INTENT", 400);
            }
            app = apps.findById(in.applicationId()).filter(Application::active)
                    .orElseThrow(() -> new SocialPageException("INVALID_CLIENT", 400));
            if (!sessions.isActive(in.sessionId())) {
                throw new SocialPageException("INVALID_INTENT", 400);
            }
            purpose = in.purpose();
            redirectUri = in.returnUrl();
            clientState = in.clientState();
            accountId = in.accountId();
            sessionId = in.sessionId();
        } else {
            app = apps.findByClientId(nz(p.clientId())).filter(Application::active)
                    .orElseThrow(() -> new SocialPageException("INVALID_CLIENT", 400));
            if (p.redirectUri() == null || !app.hasRedirectUri(p.redirectUri())) {
                throw new SocialPageException("INVALID_REDIRECT_URI", 400);
            }
            purpose = SocialTransaction.Purpose.LOGIN;
            redirectUri = p.redirectUri();
            clientState = p.state();
            if (clientState != null && clientState.length() > MAX_CLIENT_STATE) {
                throw ApiException.validation("state", "TOO_LONG");
            }
            if (!"S256".equals(p.codeChallengeMethod())) {
                throw ApiException.validation("code_challenge_method", "INVALID");
            }
            if (!Pkce.validChallenge(p.codeChallenge())) {
                throw ApiException.validation("code_challenge", "INVALID");
            }
            challenge = p.codeChallenge();
            List<String> requested = parseAudiences(p.audiences());
            audiencePolicy.forLogin(app, requested); // fora da política ⇒ erro de início (CA-FT07-20)
            audiences = requested.isEmpty() ? null : String.join(" ", requested);
        }
        if (!providerEnabled(provider, app)) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_ENABLED);
        }

        Instant now = clock.instant();
        String state = TokenGenerator.newToken();
        String nonce = provider == Provider.GOOGLE ? TokenGenerator.newToken(24) : null;
        String providerVerifier = TokenGenerator.newToken(48);
        String binding = TokenGenerator.newToken();
        SocialTransaction t = new SocialTransaction(UUID.randomUUID(), provider, purpose, app.id(), redirectUri,
                clientState, challenge, audiences, nonce, cipher.encryptString(providerVerifier, PKCE_PURPOSE),
                Sha256.hex(binding), accountId, sessionId, now, now.plus(props.social().transactionTtl()));
        tx.executeWithoutResult(s -> transactions.insert(t, Sha256.hex(state)));

        boolean reauth = purpose == SocialTransaction.Purpose.REAUTH;
        String location = client.authorizationUrl(callbackUrl(provider), state, nonce,
                Pkce.challengeFor(providerVerifier), reauth, !reauth);
        String cookie = org.springframework.http.ResponseCookie.from(bindingCookieName(), binding).httpOnly(true)
                .secure(props.cookie().secure()).sameSite("Lax").path("/auth/social")
                .maxAge(props.social().transactionTtl()).build().toString();
        return new Redirect(location, cookie);
    }

    // ------------------------------------------------------------------ UC-051
    public Redirect callback(Provider provider, String code, String state, String providerError,
                             String bindingCookie, String acceptLanguage) {
        String clear = org.springframework.http.ResponseCookie.from(bindingCookieName(), "").httpOnly(true)
                .secure(props.cookie().secure()).sameSite("Lax").path("/auth/social").maxAge(0).build().toString();
        if (state == null || state.isBlank() || state.length() > 200) {
            failEvent(provider, null, null, "unknown_state");
            throw new SocialPageException("INVALID_TRANSACTION", 400);
        }
        Instant now = clock.instant();
        SocialTransactionRepository.Consumed consumed = tx.execute(s -> transactions.consume(Sha256.hex(state), now));
        if (consumed.status() == SocialTransactionRepository.ConsumeStatus.UNKNOWN) {
            failEvent(provider, null, null, "unknown_state");
            throw new SocialPageException("INVALID_TRANSACTION", 400);
        }
        if (consumed.status() == SocialTransactionRepository.ConsumeStatus.REPLAY) {
            failEvent(provider, null, null, "replay");
            throw new SocialPageException("INVALID_TRANSACTION", 400);
        }
        SocialTransaction t = consumed.transaction();
        Application app = apps.findById(t.applicationId()).filter(Application::active).orElse(null);
        if (app == null || t.provider() != provider) {
            throw new SocialPageException("INVALID_TRANSACTION", 400);
        }
        String clientId = app.clientId();
        if (!t.expiresAt().isAfter(now)) {
            return finish(t, null, "invalid_request", clear);
        }
        if (bindingCookie == null || !ConstantTime.equals(Sha256.hex(bindingCookie), t.browserBindingHash())) {
            failEvent(provider, t.accountId(), clientId, "binding");
            return finish(t, null, "invalid_request", clear);
        }
        if (providerError != null && !providerError.isBlank()) {
            boolean denied = "access_denied".equals(providerError);
            failEvent(provider, t.accountId(), clientId, denied ? "denied" : "provider_error");
            return finish(t, null, denied ? "access_denied" : "invalid_request", clear);
        }
        if (code == null || code.isBlank() || code.length() > 2048) {
            failEvent(provider, t.accountId(), clientId, "no_code");
            return finish(t, null, "invalid_request", clear);
        }
        ProviderIdentity identity;
        try {
            String verifier = cipher.decryptString(t.providerCodeVerifierEnc(), PKCE_PURPOSE);
            identity = clients.get(provider).fetchIdentity(code, verifier, t.nonce(), callbackUrl(provider));
        } catch (ProviderException e) {
            LOG.warn("Falha no provedor {}: {}", provider.slug(), e.getMessage());
            failEvent(provider, t.accountId(), clientId, e.unavailable() ? "provider_unavailable" : "invalid_token");
            return finish(t, null, e.unavailable() ? "temporarily_unavailable" : "invalid_request", clear);
        }
        String locale = LocaleSupport.resolve(acceptLanguage, props.defaultLocale());
        Outcome o = switch (t.purpose()) {
            case LOGIN -> tx.execute(s -> processLogin(app, t, identity, locale));
            case LINK -> tx.execute(s -> processLink(app, t, identity));
            case REAUTH -> tx.execute(s -> processReauth(app, t, identity));
        };
        return finish(t, o, null, clear);
    }

    private Redirect finish(SocialTransaction t, Outcome o, String error, String clearCookie) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(t.redirectUri());
        String err = o != null ? o.error() : error;
        if (err != null) {
            b.queryParam("error", err);
        } else if (t.purpose() == SocialTransaction.Purpose.LOGIN) {
            b.queryParam("code", o.code());
        } else {
            b.queryParam("result", o.code());
        }
        if (t.clientState() != null) {
            b.queryParam("state", t.clientState());
        }
        return new Redirect(b.build().encode().toUriString(), clearCookie);
    }

    // ---- LOGIN
    private Outcome processLogin(Application app, SocialTransaction t, ProviderIdentity id, String locale) {
        Provider provider = t.provider();
        Instant now = clock.instant();
        SocialLink link = links.findByIdentity(provider, id.subject()).orElse(null);
        Account linked = link == null ? null : accounts.findById(link.accountId()).orElse(null);
        EmailAddress.Normalized email = id.email() == null ? null : EmailAddress.parse(id.email()).orElse(null);
        Account sameEmail = link == null && email != null ? accounts.findByEmail(email.normalized()).orElse(null)
                : null;
        SocialAccountDecision.Outcome decision = SocialAccountDecision.decide(
                linked == null ? null : linked.status(), email != null, id.emailVerified(),
                sameEmail == null ? null : sameEmail.status());
        String clientId = app.clientId();
        switch (decision) {
            case LOGIN_EXISTING -> {
                links.touchLogin(link.id(), now);
                return Outcome.ok(issueCode(app, t, linked.id(), provider, now));
            }
            case ERROR_ACCOUNT_DISABLED -> {
                events.recordIndependent(EventType.LOGIN_BLOCKED_DISABLED, linked.id(), clientId, Map.of());
                return Outcome.error("account_disabled");
            }
            case ERROR_EMAIL_VERIFICATION_REQUIRED -> {
                events.recordIndependent(EventType.LOGIN_BLOCKED_UNVERIFIED, linked.id(), clientId, Map.of());
                return Outcome.error("email_verification_required");
            }
            case ERROR_EMAIL_UNAVAILABLE -> {
                failEvent(provider, null, clientId, "email_unavailable");
                return Outcome.error("email_unavailable");
            }
            case ERROR_ACCOUNT_LINK_REQUIRED -> {
                events.recordIndependent(EventType.SOCIAL_LINK_CONFLICT, sameEmail.id(), clientId,
                        Map.of("provider", provider.slug()));
                return Outcome.error("account_link_required");
            }
            case NOTIFY_EXISTING_AND_REQUIRE_VERIFICATION -> {
                if (limiter.hit(RateRule.DUPLICATE_NOTICE_EMAIL, sameEmail.emailNormalized()).allowed()
                        && !sameEmail.anonymized()) {
                    Map<String, String> vars = new HashMap<>();
                    vars.put("app_name", app.name());
                    vars.put("action_url", actionLinks.uiSignIn());
                    vars.put("action_url2", actionLinks.uiForgotPassword());
                    mail.send(MailTemplate.TPL_05, sameEmail.locale(), sameEmail.emailDisplay(), sameEmail.id(),
                            app.id(), vars);
                }
                return Outcome.error("email_verification_required");
            }
            case CREATE_PENDING_AND_REQUIRE_VERIFICATION -> {
                Account fresh = newAccount(email, AccountStatus.PENDING_VERIFICATION, null, locale, now);
                if (!createWithLink(fresh, provider, id, now)) {
                    return Outcome.error("invalid_request");
                }
                registration.sendVerification(fresh, app);
                events.record(AuditEvent.of(EventType.SOCIAL_ACCOUNT_CREATED).account(fresh.id()).client(clientId)
                        .detail("provider", provider.slug()).detail("email_verified", false));
                return Outcome.error("email_verification_required");
            }
            case SUPERSEDE_PENDING_AND_CREATE_ACTIVE -> {
                accounts.deletePending(sameEmail.id());
                events.record(AuditEvent.of(EventType.PENDING_ACCOUNT_SUPERSEDED).account(sameEmail.id())
                        .client(clientId).detail("provider", provider.slug()));
                return createActive(app, t, email, id, locale, now);
            }
            case CREATE_ACTIVE -> {
                return createActive(app, t, email, id, locale, now);
            }
            default -> throw new IllegalStateException("Decisão inesperada: " + decision);
        }
    }

    private Outcome createActive(Application app, SocialTransaction t, EmailAddress.Normalized email,
                                 ProviderIdentity id, String locale, Instant now) {
        Account fresh = newAccount(email, AccountStatus.ACTIVE, now, locale, now);
        if (!createWithLink(fresh, t.provider(), id, now)) {
            return Outcome.error("invalid_request");
        }
        events.record(AuditEvent.of(EventType.SOCIAL_ACCOUNT_CREATED).account(fresh.id()).client(app.clientId())
                .detail("provider", t.provider().slug()).detail("email_verified", true));
        return Outcome.ok(issueCode(app, t, fresh.id(), t.provider(), now));
    }

    private Account newAccount(EmailAddress.Normalized email, AccountStatus status, Instant verifiedAt, String locale,
                               Instant now) {
        return new Account(UUID.randomUUID(), email.display(), email.normalized(), status, AccountRole.USER,
                verifiedAt, locale, null, null, null, now, now, 0);
    }

    private boolean createWithLink(Account a, Provider provider, ProviderIdentity id, Instant now) {
        if (!accounts.insertIfAbsent(a)) {
            return false;
        }
        return links.insert(new SocialLink(UUID.randomUUID(), a.id(), provider, id.subject(), id.email(), now, now));
    }

    private String issueCode(Application app, SocialTransaction t, UUID accountId, Provider provider, Instant now) {
        String code = TokenGenerator.newToken();
        codes.insert(new SocialExchangeCodeRepository.Code(UUID.randomUUID(), accountId, app.id(), provider,
                t.audiences(), t.redirectUri(), t.clientCodeChallenge(), now,
                now.plus(props.social().exchangeCodeTtl()), null, null), Sha256.hex(code));
        return code;
    }

    // ---- LINK / REAUTH
    private boolean sessionStillOwned(SocialTransaction t) {
        if (t.sessionId() == null || t.accountId() == null || !sessions.isActive(t.sessionId())) {
            return false;
        }
        Session s = sessions.require(t.sessionId());
        return s.accountId().equals(t.accountId());
    }

    private Outcome processLink(Application app, SocialTransaction t, ProviderIdentity id) {
        Provider provider = t.provider();
        if (!sessionStillOwned(t)) {
            failEvent(provider, t.accountId(), app.clientId(), "session_inactive");
            return Outcome.error("invalid_request");
        }
        Account a = accounts.findByIdForUpdate(t.accountId()).orElse(null);
        if (a == null || !a.active()) {
            return Outcome.error("invalid_request");
        }
        if (links.findByIdentity(provider, id.subject()).isPresent()
                || links.findByAccountAndProvider(a.id(), provider).isPresent()) {
            return Outcome.error("already_linked");
        }
        Instant now = clock.instant();
        if (!links.insert(new SocialLink(UUID.randomUUID(), a.id(), provider, id.subject(), id.email(), now, null))) {
            return Outcome.error("already_linked");
        }
        events.record(AuditEvent.of(EventType.SOCIAL_LINKED).account(a.id()).client(app.clientId())
                .session(t.sessionId()).detail("provider", provider.slug()));
        Map<String, String> vars = new HashMap<>();
        vars.put("provider", providerName(provider));
        vars.put("variant", "linked");
        vars.put("action_url", actionLinks.uiSecurity());
        mail.notice(MailTemplate.TPL_10, a.locale(), a.emailDisplay(), a.id(), now, vars);
        return Outcome.ok("linked");
    }

    private Outcome processReauth(Application app, SocialTransaction t, ProviderIdentity id) {
        Provider provider = t.provider();
        if (!sessionStillOwned(t)) {
            return Outcome.error("invalid_request");
        }
        SocialLink link = links.findByIdentity(provider, id.subject()).orElse(null);
        if (link == null || !link.accountId().equals(t.accountId())) {
            events.recordIndependent(EventType.REAUTH_FAILED, t.accountId(), app.clientId(),
                    Map.of("method", provider.slug()));
            return Outcome.error("reauth_failed");
        }
        sessions.markReauthenticated(t.sessionId());
        links.touchLogin(link.id(), clock.instant());
        events.record(AuditEvent.of(EventType.REAUTH_SUCCEEDED).account(t.accountId()).client(app.clientId())
                .session(t.sessionId()).detail("method", provider.slug()));
        return Outcome.ok("reauthenticated");
    }

    public static String providerName(Provider p) {
        return p == Provider.GOOGLE ? "Google" : "Facebook";
    }

    // ------------------------------------------------------------------ UC-052
    public TokenResponses.Result exchange(ClientContext client, String code, String verifier, String redirectUri) {
        limiter.checkAll(Ips.subject(), List.of(RateRule.AUTH_IP_MINUTE, RateRule.AUTH_IP_HOUR));
        if (code == null || code.isBlank() || code.length() > 256) {
            throw ApiException.validation("code", "REQUIRED");
        }
        if (!Pkce.validVerifier(verifier)) {
            throw ApiException.validation("code_verifier", "INVALID");
        }
        if (redirectUri == null || redirectUri.isBlank() || redirectUri.length() > 500) {
            throw ApiException.validation("redirect_uri", "REQUIRED");
        }
        Application app = client.application();
        String hash = Sha256.hex(code);
        Exchange out = tx.execute(s -> doExchange(app, hash, verifier, redirectUri));
        if (out instanceof ExchangeOk ok) {
            return ok.result();
        }
        throw new ApiException(ErrorCode.INVALID_GRANT);
    }

    private Exchange doExchange(Application app, String hash, String verifier, String redirectUri) {
        Instant now = clock.instant();
        SocialExchangeCodeRepository.Code c = codes.findByHashForUpdate(hash).orElse(null);
        if (c == null) {
            return new ExchangeFail();
        }
        if (c.consumedAt() != null) { // reuso: revoga a sessão criada pelo primeiro uso (RFC 6749 §4.1.2)
            if (c.sessionId() != null) {
                sessions.revokeById(c.sessionId(), "REUSE_DETECTED");
            }
            events.record(AuditEvent.of(EventType.SOCIAL_LOGIN_FAILED).account(c.accountId())
                    .client(app.clientId()).detail("provider", c.provider().slug()).detail("reason", "code_reuse"));
            return new ExchangeFail();
        }
        if (!c.expiresAt().isAfter(now) || !c.applicationId().equals(app.id())
                || !ConstantTime.equals(c.redirectUri(), redirectUri)
                || !Pkce.matches(verifier, c.codeChallenge())) {
            return new ExchangeFail();
        }
        Account account = accounts.findById(c.accountId()).orElse(null);
        if (account == null || !account.active() || account.anonymized()) {
            return new ExchangeFail();
        }
        List<String> audiences;
        try {
            audiences = audiencePolicy.forLogin(app, parseAudiences(c.audiences()));
        } catch (ApiException e) {
            return new ExchangeFail();
        }
        SessionService.Created created = sessions.create(account.id(), app, c.provider().amr(), audiences);
        codes.markConsumed(c.id(), created.session().id(), now);
        JwtIssuer.Issued access = jwt.issue(new JwtIssuer.Request(account.id(), created.session().id(),
                app.clientId(), audiences, c.provider().amr(), created.session().authTime(), account.admin(),
                app.accessTokenTtlSeconds()));
        events.record(AuditEvent.of(EventType.SOCIAL_LOGIN_SUCCEEDED).account(account.id()).client(app.clientId())
                .session(created.session().id()).detail("provider", c.provider().slug()));
        events.record(AuditEvent.of(EventType.TOKEN_ISSUED).account(account.id()).client(app.clientId())
                .session(created.session().id()).detail("audiences", audiences));
        return new ExchangeOk(responses.build(app, access, created.session().id(), audiences,
                created.refreshToken(), created.refreshExpiresAt(), now, true));
    }

    // ------------------------------------------------------------------ util
    private void failEvent(Provider provider, UUID accountId, String clientId, String reason) {
        events.recordIndependent(EventType.SOCIAL_LOGIN_FAILED, accountId, clientId,
                Map.of("provider", provider.slug(), "reason", reason));
    }

    static List<String> parseAudiences(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.trim().split("\\s+")).filter(s -> !s.isEmpty()).toList();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
