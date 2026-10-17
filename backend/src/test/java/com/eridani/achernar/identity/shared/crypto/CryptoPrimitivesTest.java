package com.eridani.achernar.identity.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CryptoPrimitivesTest {

    @Test
    void sha256_vetorConhecido() {
        assertThat(Sha256.hex("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void pkce_vetorDaRfc7636() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
        assertThat(Pkce.challengeFor(verifier)).isEqualTo(challenge);
        assertThat(Pkce.matches(verifier, challenge)).isTrue();
        assertThat(Pkce.matches(verifier + "x", challenge)).isFalse();
    }

    @Test
    void pkce_validaFormatoDoVerifierEDoChallenge() {
        assertThat(Pkce.validVerifier("a".repeat(43))).isTrue();
        assertThat(Pkce.validVerifier("a".repeat(42))).isFalse();
        assertThat(Pkce.validVerifier("a".repeat(129))).isFalse();
        assertThat(Pkce.validVerifier("a".repeat(43) + "!")).isFalse();
        assertThat(Pkce.validChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")).isTrue();
        assertThat(Pkce.validChallenge("curto")).isFalse();
        assertThat(Pkce.validChallenge(null)).isFalse();
    }

    @Test
    void constantTime_comparaSemVazarComprimento() {
        assertThat(ConstantTime.equals("segredo", "segredo")).isTrue();
        assertThat(ConstantTime.equals("segredo", "segredO")).isFalse();
        assertThat(ConstantTime.equals("a", "aaaa")).isFalse();
        assertThat(ConstantTime.equals(null, "x")).isFalse();
        assertThat(ConstantTime.equals("x", null)).isFalse();
    }

    @Test
    void tokenGenerator_tem43CaracteresUrlSafeEEhUnico() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String t = TokenGenerator.newToken();
            assertThat(t).hasSize(43);
            assertThat(t).matches("^[A-Za-z0-9_-]{43}$");
            seen.add(t);
        }
        assertThat(seen).hasSize(500);
        assertThat(TokenGenerator.newToken(48)).hasSize(64);
        assertThat(TokenGenerator.randomBytes(16).length).isEqualTo(16);
    }

    @Test
    void masterKeyCipher_cifraEDecifraComFinalidade() {
        MasterKeyCipher c = MasterKeyCipher.devInsecure();
        byte[] blob = c.encryptString("segredo-do-provedor", "social-pkce");
        assertThat(c.decryptString(blob, "social-pkce")).isEqualTo("segredo-do-provedor");
        assertThat(new String(blob, StandardCharsets.ISO_8859_1)).doesNotContain("segredo-do-provedor");
    }

    @Test
    void masterKeyCipher_finalidadeDiferenteOuAdulteracaoFalham() {
        MasterKeyCipher c = MasterKeyCipher.devInsecure();
        byte[] blob = c.encryptString("x", "mail-outbox");
        assertThatThrownBy(() -> c.decryptString(blob, "social-pkce")).isInstanceOf(IllegalStateException.class);
        byte[] tampered = blob.clone();
        tampered[tampered.length - 1] ^= 0x01;
        assertThatThrownBy(() -> c.decryptString(tampered, "mail-outbox")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.decrypt(new byte[4], "mail-outbox")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void masterKeyCipher_cifrasDiferentesParaMesmoTexto() {
        MasterKeyCipher c = MasterKeyCipher.devInsecure();
        assertThat(Base64.getEncoder().encodeToString(c.encryptString("igual", "p")))
                .isNotEqualTo(Base64.getEncoder().encodeToString(c.encryptString("igual", "p")));
    }

    @Test
    void masterKeyCipher_chaveBase64CurtaEhRecusada() {
        String curta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> MasterKeyCipher.fromBase64(curta)).isInstanceOf(IllegalArgumentException.class);
        String ok = Base64.getEncoder().encodeToString(new byte[32]);
        MasterKeyCipher a = MasterKeyCipher.fromBase64(ok);
        assertThat(a.decryptString(a.encryptString("t", "p"), "p")).isEqualTo("t");
    }
}
