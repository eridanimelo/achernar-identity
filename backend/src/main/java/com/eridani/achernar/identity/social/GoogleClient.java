package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.ConstantTime;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Google (OpenID Connect, PKCE S256, nonce). Hosts fixos (SEC-40). */
@Component
public class GoogleClient implements ProviderClient {

    static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final long JWKS_TTL_SECONDS = 3600;

    private final AchernarProperties props;
    private final ProviderHttp http;
    private final Clock clock;
    private volatile JWKSet jwks;
    private volatile Instant jwksAt = Instant.EPOCH;

    public GoogleClient(AchernarProperties props, ProviderHttp http, Clock clock) {
        this.props = props;
        this.http = http;
        this.clock = clock;
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public boolean configured() {
        return props.social().googleConfigured();
    }

    @Override
    public String authorizationUrl(String callbackUrl, String state, String nonce, String codeChallenge,
                                   boolean forceReauth, boolean selectAccount) {
        StringBuilder sb = new StringBuilder(AUTH_URL).append('?');
        append(sb, "response_type", "code");
        append(sb, "client_id", props.social().googleClientId());
        append(sb, "redirect_uri", callbackUrl);
        append(sb, "scope", "openid email profile");
        append(sb, "state", state);
        append(sb, "nonce", nonce);
        append(sb, "code_challenge", codeChallenge);
        append(sb, "code_challenge_method", "S256");
        if (forceReauth) {
            append(sb, "prompt", "login");
            append(sb, "max_age", "0");
        } else if (selectAccount) {
            append(sb, "prompt", "select_account");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public ProviderIdentity fetchIdentity(String code, String codeVerifier, String nonce, String callbackUrl) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", callbackUrl);
        form.put("client_id", props.social().googleClientId());
        form.put("client_secret", props.social().googleClientSecret());
        form.put("code_verifier", codeVerifier);
        Map<String, Object> token = http.postForm(TOKEN_URL, form);
        Object idToken = token.get("id_token");
        if (!(idToken instanceof String s) || s.isBlank()) {
            throw ProviderException.invalid("id_token ausente");
        }
        return validate(s, nonce);
    }

    /** Valida assinatura, {@code iss}, {@code aud}, {@code exp} e {@code nonce}; visível para testes. */
    ProviderIdentity validate(String idToken, String expectedNonce) {
        try {
            SignedJWT jwt = SignedJWT.parse(idToken);
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
                throw ProviderException.invalid("alg");
            }
            RSAKey key = findKey(jwt.getHeader().getKeyID());
            if (!jwt.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                throw ProviderException.invalid("assinatura");
            }
            return check(jwt.getJWTClaimsSet(), expectedNonce);
        } catch (java.text.ParseException | com.nimbusds.jose.JOSEException e) {
            throw ProviderException.invalid("id_token malformado");
        }
    }

    ProviderIdentity check(JWTClaimsSet c, String expectedNonce) {
        String iss = c.getIssuer();
        if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
            throw ProviderException.invalid("iss");
        }
        List<String> aud = c.getAudience();
        if (aud == null || !aud.contains(props.social().googleClientId())) {
            throw ProviderException.invalid("aud");
        }
        if (c.getExpirationTime() == null
                || c.getExpirationTime().toInstant().plus(props.clockSkew()).isBefore(clock.instant())) {
            throw ProviderException.invalid("exp");
        }
        Object nonce = c.getClaim("nonce");
        if (!(nonce instanceof String n) || expectedNonce == null || !ConstantTime.equals(n, expectedNonce)) {
            throw ProviderException.invalid("nonce");
        }
        String sub = c.getSubject();
        if (sub == null || sub.isBlank()) {
            throw ProviderException.invalid("sub");
        }
        Object email = c.getClaim("email");
        Object verified = c.getClaim("email_verified");
        boolean ok = Boolean.TRUE.equals(verified) || "true".equals(verified);
        return new ProviderIdentity(sub, email instanceof String e && !e.isBlank() ? e : null, ok);
    }

    private RSAKey findKey(String kid) {
        JWK jwk = currentSet(false).getKeyByKeyId(kid);
        if (jwk == null) {
            jwk = currentSet(true).getKeyByKeyId(kid);
        }
        if (!(jwk instanceof RSAKey rsa)) {
            throw ProviderException.invalid("kid desconhecido");
        }
        return rsa;
    }

    private synchronized JWKSet currentSet(boolean force) {
        Instant now = clock.instant();
        if (force || jwks == null || jwksAt.plusSeconds(JWKS_TTL_SECONDS).isBefore(now)) {
            try {
                jwks = JWKSet.parse(http.getJson(JWKS_URL));
                jwksAt = now;
            } catch (java.text.ParseException e) {
                throw ProviderException.invalid("JWKS do Google inválido");
            }
        }
        return jwks;
    }

    private static void append(StringBuilder sb, String k, String v) {
        sb.append(ProviderHttp.enc(k)).append('=').append(ProviderHttp.enc(v)).append('&');
    }
}
