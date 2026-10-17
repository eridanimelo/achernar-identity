package com.eridani.achernar.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eridani.achernar.identity.registry.Application;
import com.eridani.achernar.identity.registry.AudienceGrant;
import com.eridani.achernar.identity.registry.ClientType;
import com.eridani.achernar.identity.registry.RegistryStatus;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthRulesTest {

    private static final String ID_AUD = "achernar-identity";
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    private static AudienceGrant g(String aud, boolean def, boolean active) {
        return new AudienceGrant(UUID.randomUUID(), aud, def, active);
    }

    private static Application app(boolean identityAccess, AudienceGrant... grants) {
        return new Application(UUID.randomUUID(), "app", "App", null, ClientType.BROWSER, RegistryStatus.ACTIVE, true,
                identityAccess, false, false, null, null, null, null, false, NOW, NOW, 1, List.of(), List.of(),
                List.of(grants));
    }

    private static ErrorCode code(Runnable r) {
        try {
            r.run();
        } catch (ApiException e) {
            return e.errorCode();
        }
        return null;
    }

    // ---- RefreshDecision
    @Test
    void reusoTolerado_antecessorImediatoDentroDaGraca() {
        UUID cur = UUID.randomUUID();
        assertThat(RefreshDecision.decide(cur, cur, NOW, NOW.plusSeconds(10), Duration.ofSeconds(10)))
                .isEqualTo(RefreshDecision.Reuse.TOLERATED);
    }

    @Test
    void reusoDetectado_aposGraca() {
        UUID cur = UUID.randomUUID();
        assertThat(RefreshDecision.decide(cur, cur, NOW, NOW.plusSeconds(11), Duration.ofSeconds(10)))
                .isEqualTo(RefreshDecision.Reuse.DETECTED);
    }

    @Test
    void reusoDetectado_quandoNaoEhOAntecessorImediato() {
        assertThat(RefreshDecision.decide(UUID.randomUUID(), UUID.randomUUID(), NOW, NOW, Duration.ofSeconds(10)))
                .isEqualTo(RefreshDecision.Reuse.DETECTED);
        assertThat(RefreshDecision.decide(null, UUID.randomUUID(), NOW, NOW, Duration.ofSeconds(10)))
                .isEqualTo(RefreshDecision.Reuse.DETECTED);
        UUID cur = UUID.randomUUID();
        assertThat(RefreshDecision.decide(cur, cur, null, NOW, Duration.ofSeconds(10)))
                .isEqualTo(RefreshDecision.Reuse.DETECTED);
    }

    // ---- sanitize
    @Test
    void sanitizeRemoveDuplicatasPreservandoOrdem() {
        assertThat(AudienceRules.sanitize(List.of("b", "a", "b"))).containsExactly("b", "a");
        assertThat(AudienceRules.sanitize(null)).isEmpty();
    }

    @Test
    void sanitizeRejeitaExcessoEValoresInvalidos() {
        List<String> muitas = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            muitas.add("a" + i);
        }
        assertThat(code(() -> AudienceRules.sanitize(muitas))).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(code(() -> AudienceRules.sanitize(List.of(" ")))).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(code(() -> AudienceRules.sanitize(List.of("x".repeat(201))))).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // ---- login
    @Test
    void loginSemPedidoUsaPadraoAtivasMaisIdentity() {
        Application a = app(true, g("api-a", true, true), g("api-b", false, true), g("api-c", true, false));
        assertThat(AudienceRules.forLogin(a, null, ID_AUD)).containsExactly("api-a", ID_AUD);
    }

    @Test
    void loginComPedidoUsaInterseccao() {
        Application a = app(false, g("api-a", true, true), g("api-b", false, true));
        assertThat(AudienceRules.forLogin(a, List.of("api-b"), ID_AUD)).containsExactly("api-b");
    }

    @Test
    void loginRecusaAudienciaNaoPermitidaOuInativa() {
        Application a = app(false, g("api-a", true, true), g("api-off", false, false));
        assertThat(code(() -> AudienceRules.forLogin(a, List.of("api-x"), ID_AUD))).isEqualTo(ErrorCode.AUDIENCE_NOT_ALLOWED);
        assertThat(code(() -> AudienceRules.forLogin(a, List.of("api-off"), ID_AUD))).isEqualTo(ErrorCode.AUDIENCE_NOT_ALLOWED);
    }

    @Test
    void loginSemNenhumaAudienciaResultanteFalha() {
        Application a = app(false, g("api-a", false, true));
        assertThat(code(() -> AudienceRules.forLogin(a, null, ID_AUD))).isEqualTo(ErrorCode.AUDIENCE_NOT_ALLOWED);
    }

    @Test
    void loginSoIdentityBastaQuandoPermitido() {
        Application a = app(true);
        assertThat(AudienceRules.forLogin(a, null, ID_AUD)).containsExactly(ID_AUD);
    }

    // ---- refresh
    @Test
    void refreshSemPedidoMantemAsDaSessaoAtivas() {
        Application a = app(true);
        List<String> r = AudienceRules.forRefresh(a, List.of("api-a", "api-b", ID_AUD), null, ID_AUD,
                s -> !s.equals("api-b"));
        assertThat(r).containsExactly("api-a", ID_AUD);
    }

    @Test
    void refreshNaoAmplia() {
        Application a = app(true);
        assertThat(code(() -> AudienceRules.forRefresh(a, List.of("api-a"), List.of("api-z"), ID_AUD, s -> true)))
                .isEqualTo(ErrorCode.AUDIENCE_NOT_ALLOWED);
    }

    @Test
    void refreshReduzParaSubconjunto() {
        Application a = app(true);
        assertThat(AudienceRules.forRefresh(a, List.of("api-a", "api-b"), List.of("api-b"), ID_AUD, s -> true))
                .containsExactly("api-b");
    }

    @Test
    void refreshOmiteIdentitySeAplicacaoPerdeuAcesso() {
        Application a = app(false);
        assertThat(AudienceRules.forRefresh(a, List.of("api-a", ID_AUD), null, ID_AUD, s -> true))
                .containsExactly("api-a");
    }

    @Test
    void refreshSemNadaRestanteFalha() {
        Application a = app(false);
        assertThat(code(() -> AudienceRules.forRefresh(a, List.of("api-a"), null, ID_AUD, s -> false)))
                .isEqualTo(ErrorCode.AUDIENCE_NOT_ALLOWED);
        assertThat(Set.of(1)).hasSize(1);
        assertThatThrownBy(() -> AudienceRules.forRefresh(a, List.of("api-a"), null, ID_AUD, s -> false))
                .isInstanceOf(ApiException.class);
    }
}
