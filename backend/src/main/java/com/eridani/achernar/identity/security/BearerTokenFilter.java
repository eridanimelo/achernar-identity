package com.eridani.achernar.identity.security;

import com.eridani.achernar.identity.session.SessionService;
import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.eridani.achernar.identity.shared.error.ProblemWriter;
import com.eridani.achernar.identity.token.AccessTokenVerifier;
import com.eridani.achernar.identity.token.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticação Bearer da API do próprio Identity (RN-TOK-15, D-23): valida o JWT localmente, exige a audiência do
 * Identity (P-63) e confere no banco que a sessão (sid) continua ativa — revogação imediata.
 * Não é um bean: é instanciado pela cadeia de segurança para não ser registrado como filtro global do servlet.
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private final AccessTokenVerifier verifier;
    private final SessionService sessions;
    private final AchernarProperties props;
    private final ProblemWriter problems;

    public BearerTokenFilter(AccessTokenVerifier verifier, SessionService sessions, AchernarProperties props,
                             ProblemWriter problems) {
        this.verifier = verifier;
        this.sessions = sessions;
        this.props = props;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response); // sem credencial: o ponto de entrada responde 401
            return;
        }
        AccessTokenVerifier.Verified v;
        try {
            v = verifier.verify(header.substring(7).trim());
        } catch (InvalidTokenException e) {
            reject(request, response, ErrorCode.UNAUTHENTICATED);
            return;
        }
        if (!v.audiences().contains(props.uiAudience())) {
            reject(request, response, ErrorCode.UNAUTHENTICATED);
            return;
        }
        if (!sessions.isActive(v.sessionId())) {
            reject(request, response, ErrorCode.SESSION_REVOKED);
            return;
        }
        AccessPrincipal principal = new AccessPrincipal(v.accountId(), v.sessionId(), v.clientId(), v.audiences(),
                v.jti());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        MDC.put("account_id", v.accountId().toString());
        MDC.put("client_id", v.clientId());
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        response.setHeader("WWW-Authenticate", code == ErrorCode.SESSION_REVOKED
                ? "Bearer error=\"invalid_token\", error_description=\"session revoked\""
                : "Bearer error=\"invalid_token\"");
        problems.write(request, response, code);
    }
}
