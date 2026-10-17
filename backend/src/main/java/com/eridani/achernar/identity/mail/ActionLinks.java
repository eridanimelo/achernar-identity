package com.eridani.achernar.identity.mail;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import org.springframework.stereotype.Component;

/**
 * Monta links de ação. Nunca deriva do {@code Host}: usa a URL da aplicação de origem ou a UI do Identity
 * (RN-EML-04, SEC-22). O token vai apenas no fragmento.
 */
@Component
public class ActionLinks {

    private final AchernarProperties props;

    public ActionLinks(AchernarProperties props) {
        this.props = props;
    }

    public String verifyEmail(String appUrl, String token) {
        return withToken(blank(appUrl) ? props.uiBase() + "/verify-email" : appUrl, token);
    }

    public String resetPassword(String appUrl, String token) {
        return withToken(blank(appUrl) ? props.uiBase() + "/reset-password" : appUrl, token);
    }

    public String confirmEmailChange(String appUrl, String token) {
        return withToken(blank(appUrl) ? props.uiBase() + "/confirm-email-change" : appUrl, token);
    }

    public String uiSignIn() {
        return props.uiBase() + "/login";
    }

    public String uiForgotPassword() {
        return props.uiBase() + "/forgot-password";
    }

    public String uiSecurity() {
        return props.uiBase() + "/account/security";
    }

    static String withToken(String url, String token) {
        int hash = url.indexOf('#');
        String base = hash >= 0 ? url.substring(0, hash) : url;
        return base + "#token=" + token;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
