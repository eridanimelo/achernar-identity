package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.account.AccountStatus;

/** Árvore de decisão do callback {@code LOGIN} (social-login.md §4) — lógica pura. */
public final class SocialAccountDecision {

    public enum Outcome {
        /** Vínculo existente com conta ACTIVE: emitir código. */
        LOGIN_EXISTING,
        ERROR_ACCOUNT_DISABLED,
        ERROR_EMAIL_VERIFICATION_REQUIRED,
        ERROR_EMAIL_UNAVAILABLE,
        /** E-mail verificado coincide com conta ACTIVE/DISABLED: não vincula. */
        ERROR_ACCOUNT_LINK_REQUIRED,
        /** E-mail verificado coincide com conta pendente: substitui por conta social ACTIVE. */
        SUPERSEDE_PENDING_AND_CREATE_ACTIVE,
        CREATE_ACTIVE,
        /** E-mail não verificado coincide com conta existente: TPL-05 e erro neutro. */
        NOTIFY_EXISTING_AND_REQUIRE_VERIFICATION,
        /** E-mail não verificado e sem conta: cria pendente + TPL-01. */
        CREATE_PENDING_AND_REQUIRE_VERIFICATION
    }

    private SocialAccountDecision() {
    }

    /**
     * @param linkedStatus estado da conta já vinculada à identidade (nulo se identidade nova)
     * @param sameEmailStatus estado da conta local com o mesmo e-mail normalizado (nulo se não existe)
     */
    public static Outcome decide(AccountStatus linkedStatus, boolean hasEmail, boolean emailVerified,
                                 AccountStatus sameEmailStatus) {
        if (linkedStatus != null) {
            return switch (linkedStatus) {
                case ACTIVE -> Outcome.LOGIN_EXISTING;
                case DISABLED -> Outcome.ERROR_ACCOUNT_DISABLED;
                case PENDING_VERIFICATION -> Outcome.ERROR_EMAIL_VERIFICATION_REQUIRED;
            };
        }
        if (!hasEmail) {
            return Outcome.ERROR_EMAIL_UNAVAILABLE;
        }
        if (emailVerified) {
            if (sameEmailStatus == null) {
                return Outcome.CREATE_ACTIVE;
            }
            return sameEmailStatus == AccountStatus.PENDING_VERIFICATION
                    ? Outcome.SUPERSEDE_PENDING_AND_CREATE_ACTIVE : Outcome.ERROR_ACCOUNT_LINK_REQUIRED;
        }
        return sameEmailStatus != null ? Outcome.NOTIFY_EXISTING_AND_REQUIRE_VERIFICATION
                : Outcome.CREATE_PENDING_AND_REQUIRE_VERIFICATION;
    }
}
