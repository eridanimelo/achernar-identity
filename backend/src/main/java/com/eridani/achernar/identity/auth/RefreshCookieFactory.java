package com.eridani.achernar.identity.auth;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Cookie do refresh token de aplicações BROWSER (token-lifecycle §2). */
@Component
public class RefreshCookieFactory {

    private final AchernarProperties props;

    public RefreshCookieFactory(AchernarProperties props) {
        this.props = props;
    }

    public String name(String clientId) {
        return (props.cookie().secure() ? "__Secure-" : "") + "achrt_" + clientId;
    }

    public String set(String clientId, String token, Duration maxAge) {
        return base(clientId, token).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build().toString();
    }

    public String clear(String clientId) {
        return base(clientId, "").maxAge(Duration.ZERO).build().toString();
    }

    public String read(HttpServletRequest request, String clientId) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String wanted = name(clientId);
        for (Cookie c : cookies) {
            if (wanted.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder base(String clientId, String value) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name(clientId), value).httpOnly(true)
                .secure(props.cookie().secure()).sameSite("Strict").path("/auth");
        String domain = props.cookie().domain();
        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b;
    }
}
