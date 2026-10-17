package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.credential.PasswordCredentialRepository;
import com.eridani.achernar.identity.mail.ActionLinks;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ApplicationRepository;
import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.session.Session;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.web.Ips;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** FT-08: intenções de vínculo/reautenticação social e desvinculação. */
@Service
public class SocialLinkService {

    static final int MAX_STATE = 500;

    private final SocialLoginService social;
    private final SocialIntentCodec intents;
    private final SocialLinkRepository links;
    private final PasswordCredentialRepository credentials;
    private final AccountRepository accounts;
    private final ApplicationRepository apps;
    private final SessionService sessions;
    private final RateLimiter limiter;
    private final MailService mail;
    private final ActionLinks actionLinks;
    private final SecurityEventRecorder events;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public SocialLinkService(SocialLoginService social, SocialIntentCodec intents, SocialLinkRepository links,
                             PasswordCredentialRepository credentials, AccountRepository accounts,
                             ApplicationRepository apps, SessionService sessions, RateLimiter limiter,
                             MailService mail, ActionLinks actionLinks, SecurityEventRecorder events,
                             AchernarProperties props, Clock clock, TransactionTemplate tx) {
        this.social = social;
        this.intents = intents;
        this.links = links;
        this.credentials = credentials;
        this.accounts = accounts;
        this.apps = apps;
        this.sessions = sessions;
        this.limiter = limiter;
        this.mail = mail;
        this.actionLinks = actionLinks;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    /** UC-053 (LINK) e UC-055 (REAUTH). */
    public Map<String, Object> intent(AccessPrincipal who, Provider provider, String returnUrl, String state,
                                      SocialTransaction.Purpose purpose) {
        limiter.check(RateRule.SOCIAL_START_IP, Ips.subject());
        Session session = sessions.require(who.sessionId());
        if (!sessions.isActive(session.id())) {
            throw new ApiException(ErrorCode.SESSION_REVOKED);
        }
        Application app = apps.findById(session.applicationId()).filter(Application::active)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_REVOKED));
        if (state != null && state.length() > MAX_STATE) {
            throw ApiException.validation("state", "TOO_LONG");
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            throw ApiException.validation("return_url", "REQUIRED");
        }
        if (!social.providerEnabled(provider, app)) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_ENABLED);
        }
        if (!app.hasRedirectUri(returnUrl)) {
            throw new ApiException(ErrorCode.REDIRECT_URI_NOT_ALLOWED);
        }
        if (purpose == SocialTransaction.Purpose.LINK) {
            sessions.requireRecentReauth(session.id());
            if (links.findByAccountAndProvider(who.accountId(), provider).isPresent()) {
                throw new ApiException(ErrorCode.SOCIAL_ALREADY_LINKED);
            }
        } else if (links.findByAccountAndProvider(who.accountId(), provider).isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        String token = intents.encode(provider, purpose, app.id(), returnUrl, state, who.accountId(), session.id());
        String url = props.issuerBase() + "/auth/social/" + provider.slug() + "/start?intent="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        return Obj.of().with("authorization_url", url).with("expires_in", SocialIntentCodec.TTL_SECONDS);
    }

    /** UC-054 — desvincular; nunca deixa a conta sem método de acesso (RN-SOC-09). */
    public void unlink(AccessPrincipal who, Provider provider) {
        sessions.requireRecentReauth(who.sessionId());
        tx.executeWithoutResult(status -> {
            Account a = accounts.findByIdForUpdate(who.accountId())
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
            if (links.findByAccountAndProvider(a.id(), provider).isEmpty()) {
                throw new ApiException(ErrorCode.NOT_FOUND);
            }
            if (!credentials.exists(a.id()) && links.count(a.id()) <= 1) {
                throw new ApiException(ErrorCode.LAST_ACCESS_METHOD);
            }
            links.delete(a.id(), provider);
            events.record(AuditEvent.of(EventType.SOCIAL_UNLINKED).account(a.id()).client(who.clientId())
                    .session(who.sessionId()).detail("provider", provider.slug()));
            Map<String, String> vars = new HashMap<>();
            vars.put("provider", SocialLoginService.providerName(provider));
            vars.put("variant", "removed");
            vars.put("action_url", actionLinks.uiSecurity());
            mail.notice(MailTemplate.TPL_10, a.locale(), a.emailDisplay(), a.id(), clock.instant(), vars);
        });
    }
}
