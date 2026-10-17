package com.eridani.achernar.identity.registry;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * CORS dinâmico (D-41, SEC-26): a origem precisa constar nas origens de alguma aplicação ativa (ou da UI própria).
 * Sem {@code *}. Credenciais apenas nas rotas com cookie de refresh/login. CORS não é autenticação.
 */
@Component
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {

    static final Set<String> CREDENTIAL_PATHS = Set.of("/auth/refresh", "/auth/logout", "/auth/login",
            "/auth/register", "/auth/social/exchange");

    private final ApplicationRepository apps;
    private final AchernarProperties props;

    public DynamicCorsConfigurationSource(ApplicationRepository apps, AchernarProperties props) {
        this.apps = apps;
        this.props = props;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return null;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/.well-known/")) {
            CorsConfiguration c = new CorsConfiguration();
            c.setAllowedOrigins(List.of("*"));
            c.setAllowedMethods(List.of("GET", "OPTIONS"));
            c.setMaxAge(props.corsMaxAge());
            return c;
        }
        boolean apiRoute = path.startsWith("/api/");
        boolean allowed = props.uiOrigins() != null && props.uiOrigins().contains(origin)
                || apps.originRegistered(origin, apiRoute);
        if (!allowed) {
            return null;
        }
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(origin));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Achernar-Client", "Accept-Language",
                "X-Request-Id"));
        c.setExposedHeaders(List.of("X-Request-Id", "Retry-After"));
        c.setAllowCredentials(CREDENTIAL_PATHS.contains(path));
        c.setMaxAge(props.corsMaxAge());
        return c;
    }
}
