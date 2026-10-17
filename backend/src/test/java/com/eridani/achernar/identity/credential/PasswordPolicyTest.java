package com.eridani.achernar.identity.credential;

import static org.assertj.core.api.Assertions.assertThat;

import com.eridani.achernar.identity.shared.error.FieldError;
import java.util.List;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = PasswordPolicy.withBundledList(12, 128);

    private static List<String> codes(List<FieldError> errors) {
        return errors.stream().map(FieldError::code).toList();
    }

    @Test
    void aceitaSenhaLongaEIncomum() {
        assertThat(policy.check("cavalo-bateria-grampo-certo", "ana@exemplo.com")).isEmpty();
    }

    @Test
    void rejeitaVaziaEEmBranco() {
        assertThat(codes(policy.check("", null))).containsExactly("BLANK");
        assertThat(codes(policy.check("            ", null))).containsExactly("BLANK");
        assertThat(codes(policy.check(null, null))).containsExactly("BLANK");
    }

    @Test
    void rejeitaCurtaELonga() {
        assertThat(codes(policy.check("Abc!1234567", null))).contains("TOO_SHORT");
        assertThat(codes(policy.check("a1B-".repeat(40), null))).contains("TOO_LONG");
        assertThat(policy.check("x7Kq-mP2zR9t", null)).isEmpty();
    }

    @Test
    void contaCodePointsENaoUnidadesUtf16() {
        // 12 emojis = 12 code points (24 chars UTF-16): precisa ser aceito no mínimo
        String doze = "😀".repeat(12);
        assertThat(codes(policy.check(doze, null))).doesNotContain("TOO_SHORT");
    }

    @Test
    void rejeitaSenhaComum() {
        assertThat(codes(policy.check("123456789012", null))).contains("COMMON");
        assertThat(codes(policy.check("PASSWORD1234", null))).contains("COMMON");
    }

    @Test
    void rejeitaSenhaQueContemPartesDoEmail() {
        assertThat(codes(policy.check("maria.silva-2026-xyz", "maria.silva@empresa.com"))).contains("CONTAINS_EMAIL");
        assertThat(codes(policy.check("minha-empresa-forte-9", "maria@empresa.com"))).contains("CONTAINS_EMAIL");
        assertThat(codes(policy.check("cavalo-bateria-grampo", "ana@empresa.com"))).doesNotContain("CONTAINS_EMAIL");
    }

    @Test
    void partesCurtasDoEmailNaoContam() {
        assertThat(PasswordPolicy.containsEmailPart("xxabxx-longa-senha", "ab@cd.io")).isFalse();
    }

    @Test
    void normalizaNfkc() {
        // "ﬁ" (ligadura) normaliza para "fi"; senhas visualmente iguais comparam iguais
        assertThat(PasswordPolicy.normalize("ﬁm")).isEqualTo("fim");
        assertThat(PasswordPolicy.normalize(null)).isNull();
    }
}
