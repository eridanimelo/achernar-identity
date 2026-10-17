package com.eridani.achernar.identity.token;

import com.eridani.achernar.identity.shared.config.AchernarProperties;
import com.eridani.achernar.identity.shared.crypto.Sha256;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** FT-10: JWKS e metadados do emissor (UC-070). */
@RestController
public class JwksController {

    private final SigningKeyService keys;
    private final AchernarProperties props;

    public JwksController(SigningKeyService keys, AchernarProperties props) {
        this.keys = keys;
        this.props = props;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = "application/jwk-set+json")
    public ResponseEntity<String> jwks(@RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        String body = keys.jwksJson();
        String etag = "\"" + Sha256.hex(body).substring(0, 32) + "\"";
        CacheControl cc = CacheControl.maxAge(props.key().jwksCacheMaxAge().toSeconds(), TimeUnit.SECONDS).cachePublic();
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).cacheControl(cc).build();
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(cc).contentType(MediaType.parseMediaType("application/jwk-set+json"))
                .body(body);
    }

    @GetMapping(value = {"/.well-known/openid-configuration", "/.well-known/oauth-authorization-server"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> metadata() {
        String issuer = props.issuerBase();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issuer", issuer);
        m.put("jwks_uri", issuer + "/.well-known/jwks.json");
        m.put("introspection_endpoint", issuer + "/api/v1/tokens/introspect");
        m.put("introspection_endpoint_auth_methods_supported", List.of("client_secret_basic"));
        m.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "none"));
        m.put("id_token_signing_alg_values_supported", List.of("RS256"));
        m.put("subject_types_supported", List.of("public"));
        m.put("response_types_supported", List.of());
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(m);
    }
}
