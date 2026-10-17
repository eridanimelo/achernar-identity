package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.ClientAuthenticator;
import com.eridani.achernar.identity.registry.ClientContext;
import com.eridani.achernar.identity.registry.ClientType;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FT-03: API-FT03-01…03 (login, refresh, logout). */
@RestController
@RequestMapping("/auth")
public class AuthController {

    public static final String CLIENT_HEADER = "X-Achernar-Client";

    public record LoginRequest(@JsonProperty("client_id") String clientId, @JsonProperty("email") String email,
                               @JsonProperty("password") String password,
                               @JsonProperty("audiences") List<String> audiences) {
    }

    public record RefreshRequest(@JsonProperty("client_id") String clientId,
                                 @JsonProperty("refresh_token") String refreshToken,
                                 @JsonProperty("audiences") List<String> audiences) {
    }

    private final ClientAuthenticator clients;
    private final LoginService login;
    private final RefreshService refresh;
    private final LogoutService logout;
    private final RefreshCookieFactory cookies;

    public AuthController(ClientAuthenticator clients, LoginService login, RefreshService refresh,
                          LogoutService logout, RefreshCookieFactory cookies) {
        this.clients = clients;
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request, @RequestBody LoginRequest body) {
        ClientContext client = clients.authenticate(request, body.clientId(), true);
        TokenResponses.Result r = login.login(client, body.email(), body.password(), body.audiences());
        return tokens(r);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request,
                                                      @RequestBody(required = false) RefreshRequest body) {
        RefreshRequest b = body == null ? new RefreshRequest(null, null, null) : body;
        ClientContext client = refreshClient(request, b);
        Application app = client.application();
        String presented = presentedToken(request, app, b);
        try {
            return tokens(refresh.refresh(app, presented, b.audiences()));
        } catch (ApiException e) {
            if (e.errorCode() == ErrorCode.INVALID_REFRESH_TOKEN && app.type() == ClientType.BROWSER) {
                throw e.withHeader(HttpHeaders.SET_COOKIE, cookies.clear(app.clientId()));
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @RequestBody(required = false) RefreshRequest body) {
        RefreshRequest b = body == null ? new RefreshRequest(null, null, null) : body;
        ClientContext client = refreshClient(request, b);
        Application app = client.application();
        logout.logout(app, presentedToken(request, app, b));
        ResponseEntity.BodyBuilder ok = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .cacheControl(CacheControl.noStore());
        if (app.type() == ClientType.BROWSER) {
            ok.header(HttpHeaders.SET_COOKIE, cookies.clear(app.clientId()));
        }
        return ok.build();
    }

    /** Autentica o cliente; no BROWSER aplica a defesa CSRF (Origin + {@code X-Achernar-Client} + JSON). */
    private ClientContext refreshClient(HttpServletRequest request, RefreshRequest body) {
        String header = request.getHeader(CLIENT_HEADER);
        String candidate = header != null && !header.isBlank() ? header : body.clientId();
        ClientContext client = clients.authenticate(request, candidate, true);
        Application app = client.application();
        if (app.type() == ClientType.BROWSER) {
            boolean json = request.getContentType() != null
                    && MediaType.parseMediaType(request.getContentType()).isCompatibleWith(MediaType.APPLICATION_JSON);
            if (header == null || !header.equals(app.clientId()) || !json
                    || (body.clientId() != null && !body.clientId().equals(app.clientId()))) {
                throw new ApiException(ErrorCode.FORBIDDEN);
            }
        }
        return client;
    }

    private String presentedToken(HttpServletRequest request, Application app, RefreshRequest body) {
        if (app.type() == ClientType.BROWSER) {
            return cookies.read(request, app.clientId());
        }
        return body.refreshToken();
    }

    private static ResponseEntity<Map<String, Object>> tokens(TokenResponses.Result r) {
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache");
        if (r.setCookie() != null) {
            b.header(HttpHeaders.SET_COOKIE, r.setCookie());
        }
        return b.body(r.body());
    }
}
