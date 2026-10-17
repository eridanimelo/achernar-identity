package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ClientType;
import com.eridani.achernar.identity.shared.json.Obj;
import com.eridani.achernar.identity.token.JwtIssuer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Monta a resposta de tokens (api-conventions §1.2) e o cookie correspondente. */
@Component
public class TokenResponses {

    /** Resultado de autenticação/renovação: corpo JSON e, opcionalmente, cabeçalho Set-Cookie. */
    public record Result(Obj body, String setCookie) {
    }

    private final RefreshCookieFactory cookies;

    public TokenResponses(RefreshCookieFactory cookies) {
        this.cookies = cookies;
    }

    /**
     * @param refreshToken novo refresh (nulo em reuso tolerado)
     * @param refreshExpiresAt validade do refresh emitido
     */
    public Result build(Application app, JwtIssuer.Issued access, UUID sessionId, List<String> audiences,
                        String refreshToken, Instant refreshExpiresAt, Instant now, boolean rotated) {
        Obj body = Obj.of().with("token_type", "Bearer").with("access_token", access.token())
                .with("expires_in", access.ttl().toSeconds()).with("session_id", sessionId.toString())
                .with("audiences", audiences);
        String cookie = null;
        if (rotated && refreshToken != null) {
            Duration remaining = Duration.between(now, refreshExpiresAt);
            if (app.type() == ClientType.CONFIDENTIAL) {
                body.with("refresh_token", refreshToken).with("refresh_expires_in", remaining.toSeconds());
            } else {
                cookie = cookies.set(app.clientId(), refreshToken, remaining);
            }
        }
        body.with("refresh_rotated", rotated);
        return new Result(body, cookie);
    }
}
