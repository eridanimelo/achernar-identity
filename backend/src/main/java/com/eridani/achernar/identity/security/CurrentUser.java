package com.eridani.achernar.identity.security;

import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso ao principal autenticado da requisição corrente. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AccessPrincipal require() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AccessPrincipal p) {
            return p;
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
}
