package com.achernar.identity.shared.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class RequestLimits extends OncePerRequestFilter {
    private record Window(long minute, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(req.getMethod()) && req.getRequestURI().startsWith("/api/auth/")) {
            long minute = System.currentTimeMillis() / 60000;
            if (windows.size() > 10000)
                windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
            String key = req.getRemoteAddr() + ":" + req.getRequestURI();
            if (windows.size() > 20000 && !windows.containsKey(key)) {
                reject(res);
                return;
            }
            var window = windows.compute(key, (k, w) -> w == null || w.minute() != minute ? new Window(minute, 1)
                    : new Window(minute, w.count() + 1));
            if (window.count() > 15) {
                reject(res);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void reject(HttpServletResponse res) throws IOException {
        res.setStatus(429);
        res.setHeader("Retry-After", "60");
        res.setContentType("application/json");
        res.getWriter().write("{\"code\":\"RATE_LIMIT\",\"message\":\"Muitas tentativas. Aguarde um minuto.\"}");
    }
}
