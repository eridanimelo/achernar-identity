package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.audit.AuditEvent;
import com.eridani.achernar.identity.audit.EventType;
import com.eridani.achernar.identity.audit.SecurityEventRecorder;
import com.eridani.achernar.identity.mail.ActionLinks;
import com.eridani.achernar.identity.mail.MailService;
import com.eridani.achernar.identity.mail.MailTemplate;
import com.eridani.achernar.identity.ratelimit.RateLimiter;
import com.eridani.achernar.identity.ratelimit.RateRule;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ApplicationRepository;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.security.AccessPrincipal;
import com.eridani.achernar.identity.session.Session;
import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.util.EmailAddress;
import com.eridani.achernar.identity.shared.web.Ips;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** FT-05: solicitação (UC-035) e confirmação (UC-036) de alteração de e-mail. */
@Service
public class EmailChangeService {

    private final AccountRepository accounts;
    private final ApplicationRepository apps;
    private final OneTimeTokenService tokens;
    private final SessionService sessions;
    private final RateLimiter limiter;
    private final MailService mail;
    private final ActionLinks links;
    private final SecurityEventRecorder events;
    private final AchernarProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public EmailChangeService(AccountRepository accounts, ApplicationRepository apps, OneTimeTokenService tokens,
                              SessionService sessions, RateLimiter limiter, MailService mail, ActionLinks links,
                              SecurityEventRecorder events, AchernarProperties props, Clock clock,
                              TransactionTemplate tx) {
        this.accounts = accounts;
        this.apps = apps;
        this.tokens = tokens;
        this.sessions = sessions;
        this.limiter = limiter;
        this.mail = mail;
        this.links = links;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.tx = tx;
    }

    public void request(AccessPrincipal who, String newEmail, String clientId) {
        EmailAddress.Normalized parsed = EmailAddress.parse(newEmail)
                .orElseThrow(() -> ApiException.validation("new_email", "INVALID_FORMAT"));
        sessions.requireRecentReauth(who.sessionId());
        limiter.check(RateRule.EMAIL_CHANGE_ACCOUNT, who.accountId().toString());
        Session session = sessions.require(who.sessionId());
        Application app = (clientId == null || clientId.isBlank()
                ? apps.findById(session.applicationId())
                : apps.findByClientId(clientId).filter(Application::active))
                .orElseThrow(() -> ApiException.validation("client_id", "INVALID"));
        tx.executeWithoutResult(status -> {
            Account a = accounts.findByIdForUpdate(who.accountId())
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
            if (a.emailNormalized().equals(parsed.normalized())) {
                return; // igual ao atual: neutro, nenhum e-mail
            }
            boolean free = accounts.findByEmail(parsed.normalized()).isEmpty();
            Duration ttl = props.oneTime().emailChangeTtl();
            if (free) {
                OneTimeTokenService.Issued issued = tokens.issue(a.id(), TokenPurpose.EMAIL_CHANGE, app.id(),
                        parsed.display(), parsed.normalized(), ttl);
                Map<String, String> vars = new HashMap<>();
                vars.put("app_name", app.name());
                vars.put("action_url", links.confirmEmailChange(app.confirmEmailChangeUrl(), issued.token()));
                vars.put("expires_in", MailService.humanDuration(ttl, a.locale()));
                mail.send(MailTemplate.TPL_03, a.locale(), parsed.display(), a.id(), app.id(), vars);
            } else {
                tokens.invalidate(a.id(), TokenPurpose.EMAIL_CHANGE);
            }
            Map<String, String> notice = new HashMap<>();
            notice.put("action_url", links.uiForgotPassword());
            mail.notice(MailTemplate.TPL_04, a.locale(), a.emailDisplay(), a.id(), clock.instant(), notice);
            events.record(AuditEvent.of(EventType.EMAIL_CHANGE_REQUESTED).account(a.id()).client(who.clientId())
                    .session(who.sessionId()));
        });
    }

    public void confirm(ClientContext client, String token) {
        limiter.checkAll(Ips.subject(), List.of(RateRule.AUTH_IP_MINUTE, RateRule.AUTH_IP_HOUR));
        if (token == null || token.isBlank()) {
            throw ApiException.validation("token", "REQUIRED");
        }
        tx.executeWithoutResult(status -> {
            OneTimeTokenService.Consumed c = tokens.consume(token, TokenPurpose.EMAIL_CHANGE)
                    .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED));
            Account a = accounts.findByIdForUpdate(c.accountId()).orElse(null);
            if (a == null || a.status() != AccountStatus.ACTIVE || a.anonymized() || c.newEmailNormalized() == null) {
                throw new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED);
            }
            Instant now = clock.instant();
            boolean changed;
            try {
                changed = accounts.updateEmail(a.id(), c.newEmailDisplay(), c.newEmailNormalized(), now);
            } catch (DuplicateKeyException e) {
                changed = false;
            }
            if (!changed) {
                throw new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED); // rollback: sem efeito (CA-FT05-10)
            }
            sessions.revokeAllForAccount(a.id(), "EMAIL_CHANGED", null);
            tokens.invalidate(a.id(), TokenPurpose.PASSWORD_RESET);
            Map<String, String> vars = new HashMap<>();
            mail.notice(MailTemplate.TPL_07, a.locale(), a.emailDisplay(), a.id(), now, vars); // endereço antigo
            events.record(AuditEvent.of(EventType.EMAIL_CHANGED).account(a.id())
                    .client(client.application().clientId()));
        });
    }
}
