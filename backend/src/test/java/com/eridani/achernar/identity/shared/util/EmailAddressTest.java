package com.eridani.achernar.identity.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmailAddressTest {

    @Test
    void normaliza_aparaEMinusculas() {
        EmailAddress.Normalized n = EmailAddress.parse("  Maria.Silva@Exemplo.COM ").get();
        assertThat(n.display()).isEqualTo("Maria.Silva@Exemplo.COM");
        assertThat(n.normalized()).isEqualTo("maria.silva@exemplo.com");
        assertThat(n.localPart()).isEqualTo("maria.silva");
        assertThat(n.domain()).isEqualTo("exemplo.com");
    }

    @Test
    void naoDobraPontosNemRemoveTag() {
        assertThat(EmailAddress.parse("a.b+tag@exemplo.com").get().normalized()).isEqualTo("a.b+tag@exemplo.com");
    }

    @Test
    void dominioIdnVaiParaAscii() {
        assertThat(EmailAddress.parse("u@bücher.example").get().normalized()).isEqualTo("u@xn--bcher-kva.example");
    }

    @Test
    void rejeitaFormatosInvalidos() {
        String[] invalidos = {null, "", "   ", "sem-arroba", "@dominio.com", "a@", "a@@b.com", "a b@dominio.com",
            ".a@dominio.com", "a.@dominio.com", "a..b@dominio.com", "a@semponto", "a@-x.com", "a@x-.com",
            "a@dominio..com", "\"quoted\"@dominio.com", "a@dom_inio.com", "a(c)@dominio.com"};
        for (String s : invalidos) {
            Optional<EmailAddress.Normalized> r = EmailAddress.parse(s);
            assertThat(r.isEmpty()).isTrue();
        }
    }

    @Test
    void rejeitaLimitesDeTamanho() {
        String longaLocal = "a".repeat(65) + "@dominio.com";
        assertThat(EmailAddress.parse(longaLocal).isEmpty()).isTrue();
        String tudoLongo = "a@" + ("b".repeat(60) + ".").repeat(4) + "com";
        assertThat(EmailAddress.parse(tudoLongo + "x".repeat(40)).isEmpty()).isTrue();
        assertThat(EmailAddress.parse("a".repeat(64) + "@dominio.com").isPresent()).isTrue();
    }

    @Test
    void mascaraNaoVazaEnderecoCompleto() {
        assertThat(EmailAddress.mask("maria@exemplo.com")).isEqualTo("m***@e***.com");
        assertThat(EmailAddress.mask("sem-arroba")).isEqualTo("***");
        assertThat(EmailAddress.mask(null)).isNull();
    }
}
