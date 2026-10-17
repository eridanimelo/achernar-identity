package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.credential.PasswordCredentialRepository;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.shared.util.Time;
import com.eridani.achernar.identity.social.SocialLinkRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta da própria conta (UC-034). */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final SocialLinkRepository socialLinks;

    public AccountService(AccountRepository accounts, PasswordCredentialRepository credentials,
                          SocialLinkRepository socialLinks) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.socialLinks = socialLinks;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> view(UUID accountId) {
        Account a = accounts.findById(accountId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        return Obj.of().with("id", a.id().toString()).with("email", a.emailDisplay())
                .with("email_verified", a.emailVerifiedAt() != null).with("status", a.status().name())
                .with("role", a.role().name()).with("locale", a.locale())
                .with("has_password", credentials.exists(a.id()))
                .with("social_links", socialLinks.findByAccount(a.id()).stream()
                        .map(l -> Obj.of().with("provider", l.provider().slug())
                                .with("linked_at", Time.iso(l.linkedAt()))).toList())
                .with("created_at", Time.iso(a.createdAt()));
    }
}
