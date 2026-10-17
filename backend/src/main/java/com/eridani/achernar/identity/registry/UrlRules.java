package com.eridani.achernar.identity.registry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/** Regras de origens e URLs registradas (RN-APP-03/04). Lógica pura. */
public final class UrlRules {

    private UrlRules() {
    }

    /** Origem normalizada (esquema e host em minúsculas) ou vazio se inválida. */
    public static Optional<String> normalizeOrigin(String raw, boolean allowLocalHttp) {
        if (raw == null || raw.isBlank() || raw.contains("*") || !raw.equals(raw.strip())) {
            return Optional.empty();
        }
        URI uri = parse(raw);
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            return Optional.empty();
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return Optional.empty();
        }
        String path = uri.getRawPath();
        if (path != null && !path.isEmpty()) {
            return Optional.empty();
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!schemeAllowed(scheme, host, allowLocalHttp)) {
            return Optional.empty();
        }
        return Optional.of(scheme + "://" + host + (uri.getPort() >= 0 ? ":" + uri.getPort() : ""));
    }

    /**
     * Valida URL absoluta (retorno ou ação): sem fragmento nem curinga, esquema permitido e origem registrada.
     *
     * @return código de erro ({@code INVALID_FORMAT}, {@code ORIGIN_NOT_REGISTERED}) ou {@code null} se válida
     */
    public static String validateUrl(String raw, Collection<String> registeredOrigins, boolean allowLocalHttp) {
        if (raw == null || raw.isBlank() || raw.length() > 500 || raw.contains("*") || !raw.equals(raw.strip())) {
            return "INVALID_FORMAT";
        }
        URI uri = parse(raw);
        if (uri == null || uri.getScheme() == null || uri.getHost() == null || uri.getRawFragment() != null
                || raw.contains("#") || uri.getRawUserInfo() != null) {
            return "INVALID_FORMAT";
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!schemeAllowed(scheme, host, allowLocalHttp)) {
            return "INVALID_FORMAT";
        }
        String origin = scheme + "://" + host + (uri.getPort() >= 0 ? ":" + uri.getPort() : "");
        return registeredOrigins.contains(origin) ? null : "ORIGIN_NOT_REGISTERED";
    }

    private static boolean schemeAllowed(String scheme, String host, boolean allowLocalHttp) {
        if (scheme.equals("https")) {
            return true;
        }
        return scheme.equals("http") && allowLocalHttp && (host.equals("localhost") || host.equals("127.0.0.1"));
    }

    private static URI parse(String raw) {
        try {
            return new URI(raw);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
