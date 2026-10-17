package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.FieldError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converte o corpo de um PATCH (campos ausentes = inalterados; desconhecidos = erro) e de um POST (mapa bruto)
 * em {@link ApplicationInput}. Lógica pura.
 */
public final class ApplicationPatch {

    private static final Set<String> MUTABLE = Set.of("name", "description", "origins", "redirect_uris",
            "verify_email_url", "reset_password_url", "confirm_email_change_url", "allow_registration",
            "identity_api_access", "google_enabled", "facebook_enabled", "access_token_ttl_seconds", "audiences",
            "status", "version");

    private ApplicationPatch() {
    }

    public static ApplicationInput merge(Application current, Map<String, Object> patch) {
        List<FieldError> errors = new ArrayList<>();
        for (String key : patch.keySet()) {
            if (!MUTABLE.contains(key)) {
                errors.add(FieldError.of(key, "UNKNOWN_FIELD"));
            }
        }
        List<ApplicationInput.AudienceInput> currentAuds = current.audiences().stream()
                .map(a -> new ApplicationInput.AudienceInput(a.audience(), a.isDefault())).toList();
        ApplicationInput out = new ApplicationInput(
                current.clientId(),
                str(patch, "name", current.name(), errors),
                has(patch, "description") ? str(patch, "description", null, errors) : current.description(),
                current.type().name(),
                str(patch, "status", current.status().name(), errors),
                strList(patch, "origins", current.origins(), errors),
                strList(patch, "redirect_uris", current.redirectUris(), errors),
                has(patch, "verify_email_url") ? str(patch, "verify_email_url", null, errors) : current.verifyEmailUrl(),
                has(patch, "reset_password_url") ? str(patch, "reset_password_url", null, errors) : current.resetPasswordUrl(),
                has(patch, "confirm_email_change_url") ? str(patch, "confirm_email_change_url", null, errors)
                        : current.confirmEmailChangeUrl(),
                bool(patch, "allow_registration", current.allowRegistration(), errors),
                bool(patch, "identity_api_access", current.identityApiAccess(), errors),
                bool(patch, "google_enabled", current.googleEnabled(), errors),
                bool(patch, "facebook_enabled", current.facebookEnabled(), errors),
                has(patch, "access_token_ttl_seconds") ? integer(patch, "access_token_ttl_seconds", null, errors)
                        : current.accessTokenTtlSeconds(),
                audiences(patch, currentAuds, errors));
        if (out.status() != null && !out.status().equals("ACTIVE") && !out.status().equals("DISABLED")) {
            errors.add(FieldError.of("status", "INVALID"));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors);
        }
        return out;
    }

    /** POST: converte o mapa bruto em {@link ApplicationInput}. */
    public static ApplicationInput fromCreate(Map<String, Object> body) {
        List<FieldError> errors = new ArrayList<>();
        ApplicationInput in = new ApplicationInput(
                str(body, "client_id", null, errors),
                str(body, "name", null, errors),
                str(body, "description", null, errors),
                str(body, "type", null, errors),
                "ACTIVE",
                strList(body, "origins", List.of(), errors),
                strList(body, "redirect_uris", List.of(), errors),
                str(body, "verify_email_url", null, errors),
                str(body, "reset_password_url", null, errors),
                str(body, "confirm_email_change_url", null, errors),
                bool(body, "allow_registration", false, errors),
                bool(body, "identity_api_access", false, errors),
                bool(body, "google_enabled", false, errors),
                bool(body, "facebook_enabled", false, errors),
                integer(body, "access_token_ttl_seconds", null, errors),
                audiences(body, List.of(), errors));
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, errors);
        }
        return in;
    }

    public static long version(Map<String, Object> patch) {
        Object v = patch.get("version");
        if (v instanceof Number n) {
            return n.longValue();
        }
        throw ApiException.validation("version", "REQUIRED");
    }

    private static boolean has(Map<String, Object> m, String key) {
        return m.containsKey(key);
    }

    private static String str(Map<String, Object> m, String key, String fallback, List<FieldError> errors) {
        if (!m.containsKey(key)) {
            return fallback;
        }
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof String s) {
            return s;
        }
        errors.add(FieldError.of(key, "INVALID_TYPE"));
        return fallback;
    }

    private static boolean bool(Map<String, Object> m, String key, boolean fallback, List<FieldError> errors) {
        if (!m.containsKey(key) || m.get(key) == null) {
            return fallback;
        }
        if (m.get(key) instanceof Boolean b) {
            return b;
        }
        errors.add(FieldError.of(key, "INVALID_TYPE"));
        return fallback;
    }

    private static Integer integer(Map<String, Object> m, String key, Integer fallback, List<FieldError> errors) {
        if (!m.containsKey(key)) {
            return fallback;
        }
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return n.intValue();
        }
        errors.add(FieldError.of(key, "INVALID_TYPE"));
        return fallback;
    }

    private static List<String> strList(Map<String, Object> m, String key, List<String> fallback,
                                        List<FieldError> errors) {
        if (!m.containsKey(key)) {
            return fallback;
        }
        Object v = m.get(key);
        if (v == null) {
            return List.of();
        }
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof String s) {
                    out.add(s);
                } else {
                    errors.add(FieldError.of(key, "INVALID_TYPE"));
                    return fallback;
                }
            }
            return out;
        }
        errors.add(FieldError.of(key, "INVALID_TYPE"));
        return fallback;
    }

    private static List<ApplicationInput.AudienceInput> audiences(Map<String, Object> m,
                                                                  List<ApplicationInput.AudienceInput> fallback,
                                                                  List<FieldError> errors) {
        if (!m.containsKey("audiences")) {
            return fallback;
        }
        Object v = m.get("audiences");
        if (v == null) {
            return List.of();
        }
        if (!(v instanceof List<?> list)) {
            errors.add(FieldError.of("audiences", "INVALID_TYPE"));
            return fallback;
        }
        List<ApplicationInput.AudienceInput> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> map && map.get("audience") instanceof String a) {
                Object d = map.get("is_default");
                out.add(new ApplicationInput.AudienceInput(a, d instanceof Boolean b && b));
            } else {
                errors.add(FieldError.of("audiences", "INVALID_TYPE"));
                return fallback;
            }
        }
        return out;
    }
}
