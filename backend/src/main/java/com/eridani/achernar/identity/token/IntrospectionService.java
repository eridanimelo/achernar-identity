package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ApplicationRepository;
import com.eridani.achernar.identity.registry.ResourceApi;
import com.eridani.achernar.identity.session.Session;
import com.eridani.achernar.identity.session.SessionRepository;
import com.eridani.achernar.identity.shared.json.Obj;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Introspecção (UC-073, RN-TOK-11, RFC 7662): ativo ⇔ token válido ∧ {@code aud} ∋ API ∧ sessão ∧ conta ∧ aplicação ativas. */
@Service
public class IntrospectionService {

    private final AccessTokenVerifier verifier;
    private final SessionRepository sessions;
    private final AccountRepository accounts;
    private final ApplicationRepository applications;
    private final Clock clock;

    public IntrospectionService(AccessTokenVerifier verifier, SessionRepository sessions, AccountRepository accounts,
                                ApplicationRepository applications, Clock clock) {
        this.verifier = verifier;
        this.sessions = sessions;
        this.accounts = accounts;
        this.applications = applications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> introspect(ResourceApi api, String token) {
        AccessTokenVerifier.Verified v;
        try {
            v = verifier.verify(token);
        } catch (InvalidTokenException e) {
            return inactive();
        }
        if (!v.audiences().contains(api.audience())) {
            return inactive();
        }
        Session s = sessions.find(v.sessionId()).orElse(null);
        if (s == null || !s.activeAt(clock.instant()) || !s.accountId().equals(v.accountId())) {
            return inactive();
        }
        Account a = accounts.findById(v.accountId()).orElse(null);
        if (a == null || !a.active()) {
            return inactive();
        }
        Application app = applications.findById(s.applicationId()).orElse(null);
        if (app == null || !app.active()) {
            return inactive();
        }
        return Obj.of().with("active", true).with("sub", v.accountId().toString()).with("sid", v.sessionId().toString())
                .with("aud", v.audiences()).with("azp", v.clientId()).with("client_id", v.clientId())
                .with("exp", v.expiresAt()).with("iat", v.issuedAt()).with("amr", v.amr());
    }

    private static Map<String, Object> inactive() {
        return Obj.of().with("active", false);
    }
}
