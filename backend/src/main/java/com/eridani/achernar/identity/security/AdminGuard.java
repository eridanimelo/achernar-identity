package com.eridani.achernar.identity.security;

import com.eridani.achernar.identity.account.Account;
import com.eridani.achernar.identity.account.AccountRepository;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

/** Autorização administrativa verificada no banco a cada requisição (RN-ADM-01, SEC-27). */
@Component
public class AdminGuard {

    private final AccountRepository accounts;

    public AdminGuard(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public AccessPrincipal require() {
        AccessPrincipal p = CurrentUser.require();
        Account a = accounts.findById(p.accountId()).orElse(null);
        if (a == null || !a.active() || !a.admin()) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return p;
    }
}
