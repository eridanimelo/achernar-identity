package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.AudienceGrant;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Regras puras de audiências (RN-APP-17, RN-TOK-07); {@link AudiencePolicy} só fornece o contexto. */
public final class AudienceRules {

    public static final int MAX_REQUESTED = 20;
    public static final int MAX_LENGTH = 200;

    private AudienceRules() {
    }

    /** Valida formato/limites da lista pedida e remove duplicatas preservando a ordem. */
    public static List<String> sanitize(List<String> requested) {
        if (requested == null) {
            return List.of();
        }
        if (requested.size() > MAX_REQUESTED) {
            throw ApiException.validation("audiences", "TOO_MANY");
        }
        Set<String> out = new LinkedHashSet<>();
        for (String a : requested) {
            if (a == null || a.isBlank() || a.length() > MAX_LENGTH) {
                throw ApiException.validation("audiences", "INVALID_VALUE");
            }
            out.add(a);
        }
        return new ArrayList<>(out);
    }

    /** Login: (pedidas ∩ permitidas ∩ APIs ativas) ∪ {audiência do Identity se permitido}; sem pedido ⇒ padrão. */
    public static List<String> forLogin(Application app, List<String> requestedRaw, String identityAudience) {
        List<String> requested = sanitize(requestedRaw);
        Set<String> result = new LinkedHashSet<>();
        if (requested.isEmpty()) {
            for (AudienceGrant g : app.audiences()) {
                if (g.isDefault() && g.apiActive()) {
                    result.add(g.audience());
                }
            }
        } else {
            for (String r : requested) {
                AudienceGrant grant = app.audiences().stream().filter(g -> g.audience().equals(r)).findFirst()
                        .orElse(null);
                if (grant == null || !grant.apiActive()) {
                    throw new ApiException(ErrorCode.AUDIENCE_NOT_ALLOWED);
                }
                result.add(r);
            }
        }
        if (app.identityApiAccess()) {
            result.add(identityAudience);
        }
        if (result.isEmpty()) {
            throw new ApiException(ErrorCode.AUDIENCE_NOT_ALLOWED);
        }
        return new ArrayList<>(result);
    }

    /**
     * Refresh: subconjunto das audiências da sessão; APIs desativadas (segundo {@code apiActive}) são omitidas;
     * ampliar ⇒ {@code AUDIENCE_NOT_ALLOWED}.
     */
    public static List<String> forRefresh(Application app, List<String> sessionAudiences, List<String> requestedRaw,
                                          String identityAudience, Predicate<String> apiActive) {
        List<String> requested = sanitize(requestedRaw);
        List<String> base = new ArrayList<>();
        for (String a : sessionAudiences) {
            if (a.equals(identityAudience)) {
                if (app.identityApiAccess()) {
                    base.add(a);
                }
            } else if (apiActive.test(a)) {
                base.add(a);
            }
        }
        List<String> result;
        if (requested.isEmpty()) {
            result = base;
        } else {
            for (String r : requested) {
                if (!sessionAudiences.contains(r)) {
                    throw new ApiException(ErrorCode.AUDIENCE_NOT_ALLOWED);
                }
            }
            result = new ArrayList<>();
            for (String r : requested) {
                if (base.contains(r)) {
                    result.add(r);
                }
            }
        }
        if (result.isEmpty()) {
            throw new ApiException(ErrorCode.AUDIENCE_NOT_ALLOWED);
        }
        return result;
    }
}
