package com.eridani.achernar.identity.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Primeiro filtro da cadeia: define {@code request_id}/{@code trace_id} (reaproveita {@code X-Request-Id} válido),
 * resolve o IP real (P-47) e publica o {@link RequestInfo} + MDC. Limpa tudo ao final.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".REQUEST_ID";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private final TrustedProxyResolver proxyResolver;

    public RequestIdFilter(TrustedProxyResolver proxyResolver) {
        this.proxyResolver = proxyResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && VALID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
        String ip = proxyResolver.resolve(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 255) {
            ua = ua.substring(0, 255);
        }
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        RequestInfoHolder.set(new RequestInfo(requestId, ip, ua));
        MDC.put("trace_id", requestId);
        MDC.put("request_id", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("trace_id");
            MDC.remove("request_id");
            MDC.remove("client_id");
            MDC.remove("account_id");
            RequestInfoHolder.clear();
        }
    }
}
