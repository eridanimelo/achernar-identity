package com.eridani.achernar.identity.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Cabeçalhos de segurança (SEC-35) e {@code Cache-Control: no-store} nas rotas sensíveis (RN-AUT-08). */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final boolean hsts;

    public SecurityHeadersFilter(boolean hsts) {
        this.hsts = hsts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
        if (hsts) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (noStore(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }

    static boolean noStore(String path) {
        if (path == null) {
            return false;
        }
        boolean publicConfig = path.startsWith("/auth/applications/");
        return !publicConfig && (path.startsWith("/auth/") || path.startsWith("/api/") || path.startsWith("/setup/"));
    }
}
