package com.eridani.achernar.identity.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebHelpersTest {

    // ---- TrustedProxyResolver
    @Test
    void semProxiesConfiguradosIgnoraForwardedFor() {
        TrustedProxyResolver r = new TrustedProxyResolver(List.of());
        assertThat(r.resolve("203.0.113.9", "198.51.100.1")).isEqualTo("203.0.113.9");
    }

    @Test
    void proxyConfiavelUsaUltimoSaltoNaoConfiavel() {
        TrustedProxyResolver r = new TrustedProxyResolver(List.of("10.0.0.0/8", "172.16.0.0/12"));
        assertThat(r.resolve("10.1.2.3", "198.51.100.7, 10.9.9.9")).isEqualTo("198.51.100.7");
        // cliente forja o primeiro salto: vale o último não confiável da direita
        assertThat(r.resolve("10.1.2.3", "1.1.1.1, 198.51.100.7")).isEqualTo("198.51.100.7");
    }

    @Test
    void conexaoNaoConfiavelIgnoraCabecalho() {
        TrustedProxyResolver r = new TrustedProxyResolver(List.of("10.0.0.0/8"));
        assertThat(r.resolve("203.0.113.9", "1.2.3.4")).isEqualTo("203.0.113.9");
    }

    @Test
    void forwardedForInvalidoVoltaAoEnderecoRemoto() {
        TrustedProxyResolver r = new TrustedProxyResolver(List.of("10.0.0.0/8"));
        assertThat(r.resolve("10.0.0.2", "nao-e-ip")).isEqualTo("10.0.0.2");
        assertThat(r.resolve("host.example", "1.2.3.4")).isNull();
    }

    @Test
    void cidrInvalidoFalhaNaConfiguracao() {
        assertThatThrownBy(() -> new TrustedProxyResolver(List.of("10.0.0.0/99")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustedProxyResolver(List.of("nao-ip")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ipv6Confiavel() {
        TrustedProxyResolver r = new TrustedProxyResolver(List.of("fd00::/8"));
        assertThat(r.isTrusted("fd12::1")).isTrue();
        assertThat(r.isTrusted("2001:db8::1")).isFalse();
    }

    // ---- IpMasker
    @Test
    void ipMasker_aproximaIpv4ParaSlash24() {
        assertThat(IpMasker.approximate("203.0.113.77")).isEqualTo("203.0.113.0/24");
    }

    @Test
    void ipMasker_aproximaIpv6ParaSlash48() {
        assertThat(IpMasker.approximate("2001:db8:abcd:1234::1")).isEqualTo("2001:db8:abcd::/48");
    }

    @Test
    void ipMasker_naoResolveNomes() {
        assertThat(IpMasker.approximate("exemplo.com")).isNull();
        assertThat(IpMasker.approximate(null)).isNull();
    }

    // ---- UserAgentSummarizer
    @Test
    void resumeNavegadorESistema() {
        assertThat(UserAgentSummarizer.summarize(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"))
                .isEqualTo("Chrome em macOS");
        assertThat(UserAgentSummarizer.summarize("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"))
                .isEqualTo("Firefox em Windows");
        assertThat(UserAgentSummarizer.summarize("curl/8.5.0")).isEqualTo("curl");
        assertThat(UserAgentSummarizer.summarize("   ")).isNull();
        assertThat(UserAgentSummarizer.summarize(null)).isNull();
    }

    // ---- BasicAuth
    private static String basic(String raw) {
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void basicAuth_separaNoUltimoDoisPontos() {
        BasicAuth.Credentials c = BasicAuth.parse(basic("https://api.exemplo.com:segredo123")).get();
        assertThat(c.id()).isEqualTo("https://api.exemplo.com");
        assertThat(c.secret()).isEqualTo("segredo123");
        BasicAuth.Credentials d = BasicAuth.parse(basic("app-web:s3")).get();
        assertThat(d.id()).isEqualTo("app-web");
    }

    @Test
    void basicAuth_rejeitaMalformados() {
        assertThat(BasicAuth.parse(null).isEmpty()).isTrue();
        assertThat(BasicAuth.parse("Bearer abc").isEmpty()).isTrue();
        assertThat(BasicAuth.parse("Basic !!!").isEmpty()).isTrue();
        assertThat(BasicAuth.parse(basic("semdoispontos")).isEmpty()).isTrue();
        assertThat(BasicAuth.parse(basic(":soSegredo")).isEmpty()).isTrue();
        assertThat(BasicAuth.parse(basic("soId:")).isEmpty()).isTrue();
        assertThat(BasicAuth.parse("basic " + Base64.getEncoder().encodeToString("a:b".getBytes(StandardCharsets.UTF_8)))
                .isPresent()).isTrue();
    }

    // ---- LocaleSupport
    @Test
    void locale_resolveAcceptLanguage() {
        assertThat(LocaleSupport.resolve("en-US,en;q=0.9", "pt-BR")).isEqualTo("en");
        assertThat(LocaleSupport.resolve("pt-PT", "en")).isEqualTo("pt-BR");
        assertThat(LocaleSupport.resolve("fr-FR", "pt-BR")).isEqualTo("pt-BR");
        assertThat(LocaleSupport.resolve(null, "en")).isEqualTo("en");
        assertThat(LocaleSupport.isSupported("pt-BR")).isTrue();
        assertThat(LocaleSupport.isSupported("es")).isFalse();
    }
}
