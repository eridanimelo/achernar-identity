package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Validação local de access tokens (RN-TOK-14): {@code alg=RS256}, {@code typ=at+jwt}, {@code kid} conhecido,
 * assinatura, {@code iss}, {@code exp}/{@code nbf} com tolerância (P-06) e claims obrigatórias.
 * Verificações em banco (sessão, conta, aplicação) ficam com quem usa o resultado.
 */
@Component
public class AccessTokenVerifier {

    public record Verified(UUID accountId, UUID sessionId, String clientId, List<String> audiences, List<String> amr,
                           String jti, long issuedAt, long expiresAt, Map<String, Object> claims) {
    }

    private static final int MAX_TOKEN_LENGTH = 4096;

    private final SigningKeyService keys;
    private final AchernarProperties props;
    private final Clock clock;

    public AccessTokenVerifier(SigningKeyService keys, AchernarProperties props, Clock clock) {
        this.keys = keys;
        this.props = props;
        this.clock = clock;
    }

    public Verified verify(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidTokenException("format");
        }
        JWSObject jws;
        try {
            jws = JWSObject.parse(token);
        } catch (ParseException e) {
            throw new InvalidTokenException("format");
        }
        if (!JWSAlgorithm.RS256.equals(jws.getHeader().getAlgorithm())) {
            throw new InvalidTokenException("alg");
        }
        if (jws.getHeader().getType() == null || !"at+jwt".equalsIgnoreCase(jws.getHeader().getType().getType())) {
            throw new InvalidTokenException("typ");
        }
        String kid = jws.getHeader().getKeyID();
        RSAPublicKey key = kid == null ? null : keys.verificationKeys().get(kid);
        if (key == null) {
            throw new InvalidTokenException("kid");
        }
        try {
            if (!jws.verify(new RSASSAVerifier(key))) {
                throw new InvalidTokenException("signature");
            }
        } catch (JOSEException e) {
            throw new InvalidTokenException("signature");
        }
        Map<String, Object> claims = jws.getPayload().toJSONObject();
        if (claims == null) {
            throw new InvalidTokenException("payload");
        }
        if (!props.issuerBase().equals(claims.get("iss"))) {
            throw new InvalidTokenException("iss");
        }
        long now = clock.instant().getEpochSecond();
        long skew = props.clockSkew().toSeconds();
        long exp = number(claims.get("exp"), "exp");
        if (now - skew >= exp) {
            throw new InvalidTokenException("exp");
        }
        Object nbf = claims.get("nbf");
        if (nbf != null && number(nbf, "nbf") > now + skew) {
            throw new InvalidTokenException("nbf");
        }
        long iat = claims.get("iat") == null ? 0 : number(claims.get("iat"), "iat");
        List<String> aud = stringList(claims.get("aud"));
        if (aud.isEmpty()) {
            throw new InvalidTokenException("aud");
        }
        UUID sub = uuid(claims.get("sub"), "sub");
        UUID sid = uuid(claims.get("sid"), "sid");
        String azp = claims.get("azp") instanceof String s ? s : null;
        String jti = claims.get("jti") instanceof String s ? s : null;
        if (azp == null) {
            throw new InvalidTokenException("azp");
        }
        return new Verified(sub, sid, azp, aud, stringList(claims.get("amr")), jti, iat, exp, claims);
    }

    public Instant clockNow() {
        return clock.instant();
    }

    private static long number(Object o, String name) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        throw new InvalidTokenException(name);
    }

    private static UUID uuid(Object o, String name) {
        if (o instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                throw new InvalidTokenException(name);
            }
        }
        throw new InvalidTokenException(name);
    }

    private static List<String> stringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof String s) {
            out.add(s);
        } else if (o instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof String s) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
