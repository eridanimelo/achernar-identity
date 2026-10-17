package com.eridani.achernar.identity.social;

import static org.assertj.core.api.Assertions.assertThat;

import com.eridani.achernar.identity.account.AccountStatus;
import com.eridani.achernar.identity.shared.crypto.MasterKeyCipher;
import com.eridani.achernar.identity.social.SocialAccountDecision.Outcome;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SocialRulesTest {

    private static final AccountStatus ACTIVE = AccountStatus.ACTIVE;
    private static final AccountStatus DISABLED = AccountStatus.DISABLED;
    private static final AccountStatus PENDING = AccountStatus.PENDING_VERIFICATION;

    @Test
    void vinculoExistenteDependeDoEstadoDaConta() {
        assertThat(SocialAccountDecision.decide(ACTIVE, true, true, null)).isEqualTo(Outcome.LOGIN_EXISTING);
        assertThat(SocialAccountDecision.decide(DISABLED, true, true, null)).isEqualTo(Outcome.ERROR_ACCOUNT_DISABLED);
        assertThat(SocialAccountDecision.decide(PENDING, true, true, null))
                .isEqualTo(Outcome.ERROR_EMAIL_VERIFICATION_REQUIRED);
        // dados do e-mail não importam quando já há vínculo
        assertThat(SocialAccountDecision.decide(ACTIVE, false, false, ACTIVE)).isEqualTo(Outcome.LOGIN_EXISTING);
    }

    @Test
    void identidadeNovaSemEmailFalha() {
        assertThat(SocialAccountDecision.decide(null, false, false, null)).isEqualTo(Outcome.ERROR_EMAIL_UNAVAILABLE);
    }

    @Test
    void emailVerificadoSemContaCriaAtiva() {
        assertThat(SocialAccountDecision.decide(null, true, true, null)).isEqualTo(Outcome.CREATE_ACTIVE);
    }

    @Test
    void emailVerificadoComContaAtivaOuDesativadaExigeVinculoExplicito() {
        assertThat(SocialAccountDecision.decide(null, true, true, ACTIVE)).isEqualTo(Outcome.ERROR_ACCOUNT_LINK_REQUIRED);
        assertThat(SocialAccountDecision.decide(null, true, true, DISABLED)).isEqualTo(Outcome.ERROR_ACCOUNT_LINK_REQUIRED);
    }

    @Test
    void emailVerificadoComContaPendenteSubstituiAPendente() {
        assertThat(SocialAccountDecision.decide(null, true, true, PENDING))
                .isEqualTo(Outcome.SUPERSEDE_PENDING_AND_CREATE_ACTIVE);
    }

    @Test
    void emailNaoVerificadoNuncaVinculaNemCriaAtiva() {
        assertThat(SocialAccountDecision.decide(null, true, false, null))
                .isEqualTo(Outcome.CREATE_PENDING_AND_REQUIRE_VERIFICATION);
        for (AccountStatus s : AccountStatus.values()) {
            assertThat(SocialAccountDecision.decide(null, true, false, s))
                    .isEqualTo(Outcome.NOTIFY_EXISTING_AND_REQUIRE_VERIFICATION);
        }
    }

    // ---- SocialIntentCodec
    static final class MutableClock extends Clock {
        Instant now;
        MutableClock(Instant now) { this.now = now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void intencaoFazIdaEVolta() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-01T00:00:00Z"));
        SocialIntentCodec codec = new SocialIntentCodec(MasterKeyCipher.devInsecure(), clock);
        UUID app = UUID.randomUUID();
        UUID acc = UUID.randomUUID();
        UUID ses = UUID.randomUUID();
        String token = codec.encode(Provider.GOOGLE, SocialTransaction.Purpose.LINK, app,
                "https://app.exemplo.com/conta", "estado-123", acc, ses);
        SocialIntentCodec.Intent i = codec.decode(token).get();
        assertThat(i.provider()).isEqualTo(Provider.GOOGLE);
        assertThat(i.purpose()).isEqualTo(SocialTransaction.Purpose.LINK);
        assertThat(i.applicationId()).isEqualTo(app);
        assertThat(i.returnUrl()).isEqualTo("https://app.exemplo.com/conta");
        assertThat(i.clientState()).isEqualTo("estado-123");
        assertThat(i.accountId()).isEqualTo(acc);
        assertThat(i.sessionId()).isEqualTo(ses);
    }

    @Test
    void intencaoExpiraEm120s() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-01T00:00:00Z"));
        SocialIntentCodec codec = new SocialIntentCodec(MasterKeyCipher.devInsecure(), clock);
        String token = codec.encode(Provider.FACEBOOK, SocialTransaction.Purpose.REAUTH, UUID.randomUUID(), "https://a.com/x",
                null, UUID.randomUUID(), UUID.randomUUID());
        clock.now = clock.now.plusSeconds(119);
        assertThat(codec.decode(token).isPresent()).isTrue();
        clock.now = clock.now.plusSeconds(1);
        assertThat(codec.decode(token).isEmpty()).isTrue();
    }

    @Test
    void intencaoAdulteradaOuInvalidaEhRejeitada() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-01T00:00:00Z"));
        SocialIntentCodec codec = new SocialIntentCodec(MasterKeyCipher.devInsecure(), clock);
        String token = codec.encode(Provider.GOOGLE, SocialTransaction.Purpose.LINK, UUID.randomUUID(), "https://a.com/x",
                null, UUID.randomUUID(), UUID.randomUUID());
        char c = token.charAt(token.length() - 3);
        String bad = token.substring(0, token.length() - 3) + (c == 'A' ? 'B' : 'A') + token.substring(token.length() - 2);
        assertThat(codec.decode(bad).isEmpty()).isTrue();
        assertThat(codec.decode(null).isEmpty()).isTrue();
        assertThat(codec.decode("").isEmpty()).isTrue();
        assertThat(codec.decode("lixo-lixo").isEmpty()).isTrue();
        assertThat(codec.decode("a".repeat(2049)).isEmpty()).isTrue();
        // chave-mestra diferente não decifra
        byte[] other = new byte[32];
        other[0] = 7;
        SocialIntentCodec outro = new SocialIntentCodec(MasterKeyCipher.fromBase64(
                java.util.Base64.getEncoder().encodeToString(other)), clock);
        assertThat(outro.decode(token).isEmpty()).isTrue();
    }
}
