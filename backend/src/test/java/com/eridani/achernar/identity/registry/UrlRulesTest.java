package com.eridani.achernar.identity.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UrlRulesTest {

    @Test
    void normalizaOrigemParaMinusculas() {
        assertThat(UrlRules.normalizeOrigin("HTTPS://App.Exemplo.COM", false).get()).isEqualTo("https://app.exemplo.com");
        assertThat(UrlRules.normalizeOrigin("https://app.exemplo.com:8443", false).get())
                .isEqualTo("https://app.exemplo.com:8443");
    }

    @Test
    void rejeitaOrigensInvalidas() {
        String[] ruins = {null, "", " https://a.com", "https://a.com ", "https://*.a.com", "https://a.com/",
            "https://a.com/x", "https://a.com?x=1", "https://a.com#f", "https://u:p@a.com", "ftp://a.com", "a.com",
            "http://a.com", "http://localhost"};
        for (String o : ruins) {
            assertThat(UrlRules.normalizeOrigin(o, false).isEmpty()).isTrue();
        }
    }

    @Test
    void httpSomenteParaLoopbackQuandoPermitido() {
        assertThat(UrlRules.normalizeOrigin("http://localhost:4200", true).get()).isEqualTo("http://localhost:4200");
        assertThat(UrlRules.normalizeOrigin("http://127.0.0.1:4200", true).isPresent()).isTrue();
        assertThat(UrlRules.normalizeOrigin("http://exemplo.com", true).isEmpty()).isTrue();
        assertThat(UrlRules.normalizeOrigin("http://localhost:4200", false).isEmpty()).isTrue();
    }

    @Test
    void urlPrecisaTerOrigemRegistrada() {
        List<String> origens = List.of("https://app.exemplo.com");
        assertThat(UrlRules.validateUrl("https://app.exemplo.com/auth/verify?x=1", origens, false)).isNull();
        assertThat(UrlRules.validateUrl("https://outro.com/x", origens, false)).isEqualTo("ORIGIN_NOT_REGISTERED");
        assertThat(UrlRules.validateUrl("https://app.exemplo.com/x#frag", origens, false)).isEqualTo("INVALID_FORMAT");
        assertThat(UrlRules.validateUrl("https://app.exemplo.com/*", origens, false)).isEqualTo("INVALID_FORMAT");
        assertThat(UrlRules.validateUrl("https://u@app.exemplo.com/x", origens, false)).isEqualTo("INVALID_FORMAT");
        assertThat(UrlRules.validateUrl("javascript:alert(1)", origens, false)).isEqualTo("INVALID_FORMAT");
        assertThat(UrlRules.validateUrl("https://app.exemplo.com/" + "a".repeat(500), origens, false))
                .isEqualTo("INVALID_FORMAT");
    }

    @Test
    void naoEnganaPorUserinfoNemPorSubdominioParecido() {
        List<String> origens = List.of("https://app.exemplo.com");
        assertThat(UrlRules.validateUrl("https://app.exemplo.com.evil.com/x", origens, false))
                .isEqualTo("ORIGIN_NOT_REGISTERED");
        assertThat(UrlRules.validateUrl("https://app.exemplo.com@evil.com/x", origens, false)).isEqualTo("INVALID_FORMAT");
    }
}
