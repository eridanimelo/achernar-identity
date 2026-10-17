package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.error.FieldError;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Validação de campos de aplicação (FT-09). Lógica pura e testável. */
public final class ApplicationValidator {

    public static final Pattern CLIENT_ID = Pattern.compile("^[a-z0-9-]{3,64}$");
    public static final int MAX_LIST = 20;

    public record Result(List<FieldError> errors, List<String> origins, List<String> redirectUris) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    private ApplicationValidator() {
    }

    /**
     * @param creating        valida também {@code client_id} e {@code type}
     * @param allowLocalHttp  aceita {@code http://localhost} (perfil dev)
     * @param googleGlobal    provedor Google configurado globalmente
     * @param facebookGlobal  provedor Facebook configurado globalmente
     */
    public static Result validate(ApplicationInput in, boolean creating, boolean allowLocalHttp,
                                  boolean googleGlobal, boolean facebookGlobal) {
        List<FieldError> errors = new ArrayList<>();
        if (creating) {
            if (in.clientId() == null || !CLIENT_ID.matcher(in.clientId()).matches()) {
                errors.add(FieldError.of("client_id", "INVALID_FORMAT"));
            }
            if (in.type() == null || !(in.type().equals("BROWSER") || in.type().equals("CONFIDENTIAL"))) {
                errors.add(FieldError.of("type", "INVALID"));
            }
        }
        if (in.name() == null || in.name().isBlank()) {
            errors.add(FieldError.of("name", "REQUIRED"));
        } else if (in.name().strip().length() > 100) {
            errors.add(FieldError.of("name", "TOO_LONG"));
        }
        if (in.description() != null && in.description().length() > 500) {
            errors.add(FieldError.of("description", "TOO_LONG"));
        }
        if (in.accessTokenTtlSeconds() != null
                && (in.accessTokenTtlSeconds() < 300 || in.accessTokenTtlSeconds() > 900)) {
            errors.add(FieldError.of("access_token_ttl_seconds", "OUT_OF_RANGE"));
        }

        List<String> origins = new ArrayList<>();
        List<String> rawOrigins = in.origins() == null ? List.of() : in.origins();
        if (rawOrigins.size() > MAX_LIST) {
            errors.add(FieldError.of("origins", "TOO_MANY"));
        } else {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < rawOrigins.size(); i++) {
                var n = UrlRules.normalizeOrigin(rawOrigins.get(i), allowLocalHttp);
                if (n.isEmpty()) {
                    errors.add(FieldError.of("origins[" + i + "]", "INVALID_FORMAT"));
                } else if (seen.add(n.get())) {
                    origins.add(n.get());
                }
            }
        }
        boolean browser = "BROWSER".equals(in.type());
        if (browser && origins.isEmpty() && rawOrigins.isEmpty()) {
            errors.add(FieldError.of("origins", "REQUIRED"));
        }

        List<String> redirects = new ArrayList<>();
        List<String> rawRedirects = in.redirectUris() == null ? List.of() : in.redirectUris();
        if (rawRedirects.size() > MAX_LIST) {
            errors.add(FieldError.of("redirect_uris", "TOO_MANY"));
        } else {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < rawRedirects.size(); i++) {
                String code = UrlRules.validateUrl(rawRedirects.get(i), origins, allowLocalHttp);
                if (code != null) {
                    errors.add(FieldError.of("redirect_uris[" + i + "]", code));
                } else if (seen.add(rawRedirects.get(i))) {
                    redirects.add(rawRedirects.get(i));
                }
            }
        }
        checkAction(errors, "verify_email_url", in.verifyEmailUrl(), origins, allowLocalHttp);
        checkAction(errors, "reset_password_url", in.resetPasswordUrl(), origins, allowLocalHttp);
        checkAction(errors, "confirm_email_change_url", in.confirmEmailChangeUrl(), origins, allowLocalHttp);

        if (in.googleEnabled() && !googleGlobal) {
            errors.add(FieldError.of("google_enabled", "PROVIDER_NOT_CONFIGURED"));
        }
        if (in.facebookEnabled() && !facebookGlobal) {
            errors.add(FieldError.of("facebook_enabled", "PROVIDER_NOT_CONFIGURED"));
        }

        List<ApplicationInput.AudienceInput> auds = in.audiences() == null ? List.of() : in.audiences();
        if (auds.size() > MAX_LIST) {
            errors.add(FieldError.of("audiences", "TOO_MANY"));
        } else {
            Set<String> seen = new HashSet<>();
            boolean anyDefault = false;
            for (int i = 0; i < auds.size(); i++) {
                ApplicationInput.AudienceInput a = auds.get(i);
                if (a == null || a.audience() == null || a.audience().isBlank() || a.audience().length() > 200) {
                    errors.add(FieldError.of("audiences[" + i + "].audience", "INVALID"));
                } else if (!seen.add(a.audience())) {
                    errors.add(FieldError.of("audiences[" + i + "].audience", "DUPLICATE"));
                }
                if (a != null && a.isDefault()) {
                    anyDefault = true;
                }
            }
            if (!auds.isEmpty() && !anyDefault) {
                errors.add(FieldError.of("audiences", "DEFAULT_REQUIRED"));
            }
            if (auds.isEmpty() && !in.identityApiAccess()) {
                errors.add(FieldError.of("audiences", "REQUIRED"));
            }
        }
        return new Result(errors, origins, redirects);
    }

    private static void checkAction(List<FieldError> errors, String field, String value, List<String> origins,
                                    boolean allowLocalHttp) {
        if (value == null || value.isBlank()) {
            return;
        }
        String code = UrlRules.validateUrl(value, origins, allowLocalHttp);
        if (code != null) {
            errors.add(FieldError.of(field, code));
        }
    }
}
