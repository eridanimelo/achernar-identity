package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Emissão de access tokens JWT RS256 (`typ=at+jwt`) — D-04, D-05, RN-TOK-01. */
@Component
public class JwtIssuer {

    public record Issued(String token, Instant expiresAt, Duration ttl, String jti) {
    }

    public record Request(UUID accountId, UUID sessionId, String clientId, List<String> audiences, String amr,
                          Instant authTime, boolean admin, Integer applicationTtlSeconds) {
    }

    private final SigningKeyService keys;
    private final AchernarProperties props;
    private final Clock clock;

    public JwtIssuer(SigningKeyService keys, AchernarProperties props, Clock clock) {
        this.keys = keys;
        this.props = props;
        this.clock = clock;
    }

    public Issued issue(Request r) {
        Duration ttl = r.applicationTtlSeconds() != null ? Duration.ofSeconds(r.applicationTtlSeconds())
                : props.token().accessTtl();
        Instant now = clock.instant();
        Instant exp = now.plus(ttl);
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", props.issuerBase());
        claims.put("sub", r.accountId().toString());
        claims.put("aud", List.copyOf(r.audiences())); // sempre array (token-lifecycle §1)
        claims.put("exp", exp.getEpochSecond());
        claims.put("iat", now.getEpochSecond());
        claims.put("nbf", now.getEpochSecond());
        claims.put("jti", jti);
        claims.put("sid", r.sessionId().toString());
        claims.put("azp", r.clientId());
        claims.put("amr", List.of(r.amr()));
        claims.put("auth_time", r.authTime().getEpochSecond());
        if (r.admin() && r.audiences().contains(props.uiAudience())) {
            claims.put("roles", List.of("ADMIN"));
        }
        SigningKeyService.Signer signer = keys.activeSigner();
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.kid())
                    .type(new JOSEObjectType("at+jwt")).build();
            JWSObject jws = new JWSObject(header, new Payload(claims));
            jws.sign(new RSASSASigner(signer.privateKey()));
            return new Issued(jws.serialize(), exp, ttl, jti);
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao assinar o access token", e);
        }
    }
}
