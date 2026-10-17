package com.eridani.achernar.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthAndKeysIT extends AbstractIT {

    @Test
    void jwksPublicaChaveRs256ComKidEMetadados() {
        Resp r = get("/.well-known/jwks.json");
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.header("Cache-Control")).contains("max-age");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) r.json().get("keys");
        assertThat(keys).isNotEmpty();
        Map<String, Object> k = keys.get(0);
        assertThat(k.get("kty")).isEqualTo("RSA");
        assertThat(k.get("alg")).isEqualTo("RS256");
        assertThat(k.get("use")).isEqualTo("sig");
        assertThat(k.get("kid")).isNotNull();
        assertThat(k.containsKey("d")).isFalse(); // nunca expõe a parte privada
        assertThat(k.containsKey("p")).isFalse();
    }

    @Test
    void jwksPermiteCorsDeQualquerOrigem() {
        Resp r = get("/.well-known/jwks.json", "Origin", "https://qualquer.exemplo");
        assertThat(r.header("Access-Control-Allow-Origin")).isEqualTo("*");
    }

    @Test
    void descobertaDeMetadados() {
        Resp r = get("/.well-known/openid-configuration");
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.str("jwks_uri")).endsWith("/.well-known/jwks.json");
        assertThat(r.str("issuer")).isNotNull();
    }

    @Test
    void statusDaInstalacaoEhPublico() {
        Resp r = get("/setup/status");
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.json().containsKey("setup_required")).isTrue();
    }

    @Test
    void rotaInexistenteRetornaProblemJson() {
        Resp r = get("/nao-existe");
        assertThat(r.status()).isEqualTo(404);
        assertThat(r.header("Content-Type")).contains("application/problem+json");
    }

    @Test
    void rotaProtegidaSemTokenRetorna401() {
        Resp r = get("/api/v1/account");
        assertThat(r.status()).isEqualTo(401);
        assertThat(r.header("WWW-Authenticate")).contains("Bearer");
    }
}
