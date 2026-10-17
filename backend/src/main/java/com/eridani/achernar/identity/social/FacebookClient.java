package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Facebook (OAuth 2.0 + Graph API). O e-mail do Facebook nunca é tratado como verificado (D-20). */
@Component
public class FacebookClient implements ProviderClient {

    private final AchernarProperties props;
    private final ProviderHttp http;

    public FacebookClient(AchernarProperties props, ProviderHttp http) {
        this.props = props;
        this.http = http;
    }

    @Override
    public Provider provider() {
        return Provider.FACEBOOK;
    }

    @Override
    public boolean configured() {
        return props.social().facebookConfigured();
    }

    private String version() {
        return props.social().facebookGraphVersion();
    }

    @Override
    public String authorizationUrl(String callbackUrl, String state, String nonce, String codeChallenge,
                                   boolean forceReauth, boolean selectAccount) {
        StringBuilder sb = new StringBuilder("https://www.facebook.com/").append(version()).append("/dialog/oauth?");
        append(sb, "client_id", props.social().facebookAppId());
        append(sb, "redirect_uri", callbackUrl);
        append(sb, "state", state);
        append(sb, "response_type", "code");
        append(sb, "scope", "public_profile,email");
        append(sb, "code_challenge", codeChallenge);
        append(sb, "code_challenge_method", "S256");
        if (forceReauth) {
            append(sb, "auth_type", "reauthenticate");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public ProviderIdentity fetchIdentity(String code, String codeVerifier, String nonce, String callbackUrl) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", props.social().facebookAppId());
        form.put("client_secret", props.social().facebookAppSecret());
        form.put("redirect_uri", callbackUrl);
        form.put("code", code);
        form.put("code_verifier", codeVerifier);
        Map<String, Object> token = http.postForm(
                "https://graph.facebook.com/" + version() + "/oauth/access_token", form);
        Object at = token.get("access_token");
        if (!(at instanceof String accessToken) || accessToken.isBlank()) {
            throw ProviderException.invalid("access_token ausente");
        }
        String url = "https://graph.facebook.com/" + version() + "/me?fields=id,email,name&access_token="
                + ProviderHttp.enc(accessToken) + "&appsecret_proof=" + proof(accessToken);
        Map<String, Object> me = http.getJson(url);
        Object id = me.get("id");
        if (!(id instanceof String subject) || subject.isBlank()) {
            throw ProviderException.invalid("id ausente");
        }
        Object email = me.get("email");
        return new ProviderIdentity(subject, email instanceof String e && !e.isBlank() ? e : null, false);
    }

    /** {@code appsecret_proof} = HMAC-SHA256(access_token, app_secret) em hexadecimal. */
    String proof(String accessToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.social().facebookAppSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] out = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void append(StringBuilder sb, String k, String v) {
        sb.append(ProviderHttp.enc(k)).append('=').append(ProviderHttp.enc(v)).append('&');
    }
}
